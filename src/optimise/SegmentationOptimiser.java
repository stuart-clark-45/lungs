package optimise;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jenetics.DoubleChromosome;
import org.jenetics.DoubleGene;
import org.jenetics.Genotype;
import org.jenetics.util.Factory;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.SegOpt;
import core.Lungs;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.ConfigHelper;
import util.DataFilter;
import util.MatUtils;
import util.MongoHelper;
import vision.BilateralFilter;
import vision.BlobDetector;
import vision.Matcher;
import vision.ROIExtractor;

/**
 * Used to optimise the parameters that are used to segment only focuses of maximising the
 * segmentation of true positives.
 *
 * @author Stuart Clark
 */
public class SegmentationOptimiser extends Optimiser<DoubleGene, Double> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SegmentationOptimiser.class);

  /*
   * Indexes in genotype for parameters to be optimised
   */
  private static final int SIGMA_COLOUR = 0;
  private static final int SIGMA_SPACE = 1;
  private static final int KERNEL_SIZE = 2;
  private static final int SURE_FG = 3;
  private static final int SURE_BG_FRAC = 4;
  private static final int HOOD_WIDTH = 5;
  private static final int HOOD_HEIGHT = 6;
  private static final int HOOD_DEPTH = 7;
  private static final int DOG_THRESH = 8;
  private static final int GRADIENT_THRESH = 9;
  private static final int NUM_SIGMA = 10;

  /**
   * The {@link Mat}s to segment.
   */
  private List<Mat> mats;

  /**
   * The points list of {@link Point}s that must be included in the segmentation.
   */
  private List<List<GroundTruth>> groundTruths;

  /**
   * The total number of {@link GroundTruth}s used in the fitness function.
   */
  private int totalGTs;

  /**
   * @param generations the maximum number of generations that should be used.
   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
   */
  public SegmentationOptimiser(int popSize, int generations, int numStacks) {
    super(popSize, generations);
    this.mats = new ArrayList<>();
    this.groundTruths = new ArrayList<>();
    DataFilter filter = DataFilter.get();

    // Load some stacks
    Datastore ds = MongoHelper.getDataStore();
    Query<CTStack> query = filter.all(ds.createQuery(CTStack.class));
    List<CTStack> stacks = query.asList(new FindOptions().limit(numStacks));

    // For each slice in all the stacks
    for (CTStack stack : stacks) {
      for (CTSlice slice : stack.getSlices()) {

        // Find all the first readings for the slice that contain a nodule. Only one reading
        // is used as we need to know exactingly how many nodules there are in the set of Mats we
        // will use
        List<GroundTruth> gtList =
            filter.singleReading(
                ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                    .field("imageSopUID").equal(slice.getImageSopUID())).asList();

        // If there are nodules in the slice
        if (!gtList.isEmpty()) {

          // Add Mat for slice into list that will be used in eval(..)
          mats.add(MatUtils.getSliceMat(slice));

          // Add gtLists to list that will be used in eval(..)
          groundTruths.add(gtList);
          totalGTs += gtList.size();
        }

      }
    }

    LOGGER.info(mats.size() + " Mats will be used in eval(..)");
  }


  /**
   * Determine the fitness of {@code gt} by calculating how accurately is segments the
   *
   * @param gt
   * @return the fitness of {@code gt}.
   */
  @Override
  protected Double eval(Genotype<DoubleGene> gt) {
    // Create the filter
    BilateralFilter filter =
        new BilateralFilter(getInt(gt, KERNEL_SIZE), getInt(gt, SIGMA_COLOUR), getInt(gt,
            SIGMA_SPACE));

    // Create the ROI extractor
    ROIExtractor extractor = new ROIExtractor(getInt(gt, SURE_FG), getDouble(gt, SURE_BG_FRAC));

    // Create the blob detector
    int[] neighbourhood =
        new int[] {getInt(gt, HOOD_WIDTH), getInt(gt, HOOD_HEIGHT), getInt(gt, HOOD_DEPTH)};
    BlobDetector detector =
        new BlobDetector(neighbourhood, getInt(gt, DOG_THRESH), getInt(gt, GRADIENT_THRESH),
            getInt(gt, NUM_SIGMA));

    // Segment the Mats
    Lungs lungs = new Lungs(filter, extractor, detector);

    // Extract the ROIs for the mats. Each sublist contains all the ROIs for the corresponding Mat
    // in segmented
    List<List<ROI>> allROIs = new ArrayList<>();
    for (Mat mat : mats) {
      List<ROI> rois = lungs.extractRois(mat);
      allROIs.add(rois);
    }

    return noduleInclusion(allROIs);
  }

  /**
   * @param allROIs a list of lists of {@link ROI}s each sublist should be all of the {@link ROI}s
   *        for the corresponding Mat in {@code this.mats}.
   * @return a {@code double} between 0 and 1 inclusive that indicated how well nodules were
   *         included in the segmented Mat. 1 meaning perfect inclusion 0 meaning no inclusion at
   *         all.
   */
  private double noduleInclusion(List<List<ROI>> allROIs) {
    double noduleInclusion = 0.0;
    for (int i = 0; i < allROIs.size(); i++) {
      // Get ground truths for segmented mat
      List<GroundTruth> segGts = groundTruths.get(i);
      // Get ROIs for segmented mat
      List<ROI> rois = allROIs.get(i);

      // Find the bestScore for each of the GroundTruths using Matcher
      for (GroundTruth segGt : segGts) {

        double bestScore = 0.0;
        for (ROI roi : rois) {
          double score = Matcher.match(roi, segGt);
          if (score > bestScore) {
            bestScore = score;
          }
        }

        // Add to noduleInclusion
        noduleInclusion += bestScore;
      }

    }

    // Normalise to value between 0 and 1 inclusive and return
    return noduleInclusion / totalGTs;
  }

  @Override
  protected String gtToString(Genotype<DoubleGene> gt) {
    return "\n# Size of the kernel used by the bilateral filter\n"
        + "segmentation.filter.kernelsize = "
        + getDouble(gt, KERNEL_SIZE)
        + "\n"
        + "# Sigma for colour used by the bilateral filter\n"
        + "segmentation.filter.sigmacolor = "
        + getDouble(gt, SIGMA_COLOUR)
        + "\n"
        + "# Sigma for space used by the bilateral filter\n"
        + "segmentation.filter.sigmaspace = "
        + getDouble(gt, SIGMA_SPACE)
        + "\n"
        + "# The threshold used to obtain the sure foreground\n"
        + "segmentation.surefg = "
        + getDouble(gt, SURE_FG)
        + "\n"
        + "# The threshold used to obtain the sure background\n"
        + "segmentation.surebgFraction = "
        + getDouble(gt, SURE_BG_FRAC)
        + "\n"
        + "# The width of the neighbourhood used when checking if a point in sigma space is a local extrema.\n"
        + "segmentation.blob.neighbourhoodWidth = "
        + getDouble(gt, HOOD_WIDTH)
        + "\n"
        + "# The height of the neighbourhood used when checking if a point in sigma space is a local extrema.\n"
        + "segmentation.blob.neighbourhoodHeight = "
        + getDouble(gt, HOOD_HEIGHT)
        + "\n"
        + "# The depth of the neighbourhood used when checking if a point in sigma space is a local extrema.\n"
        + "segmentation.blob.neighbourhoodDepth = "
        + getDouble(gt, HOOD_DEPTH)
        + "\n"
        + "# The threshold used when deciding if a point in sigma space could be a key point (values higher\n"
        + "# than this can be key points)\n"
        + "segmentation.blob.dogThresh = "
        + getDouble(gt, DOG_THRESH)
        + "\n"
        + "# The threshold used when deciding if a key point is an edge (and hence should be filtered out)\n"
        + "segmentation.blob.gradientThresh = "
        + getDouble(gt, GRADIENT_THRESH)
        + "\n"
        + "# The number of differnt sigma values to use when computing DOG\n"
        + "segmentation.blob.numSigma = " + getDouble(gt, NUM_SIGMA);
  }

  /**
   * @param gt
   * @param index
   * @return the double value for the chromosome at {@code index}.
   */
  private double getDouble(Genotype<DoubleGene> gt, int index) {
    return gt.getChromosome(index).getGene().doubleValue();
  }

  /**
   * @param gt
   * @param index
   * @return the integer value for the chromosome at {@code index}.
   */
  private int getInt(Genotype<DoubleGene> gt, int index) {
    return (int) Math.round(gt.getChromosome(index).getGene().doubleValue());
  }

  @Override
  protected String name() {
    return "Segmentation Optimiser";
  }

  @Override
  protected String populationFile() {
    return "seg-opt-population.xml";
  }

  @Override
  protected Factory<Genotype<DoubleGene>> factory() {
    return Genotype.of(
    // Sigma Colour
        DoubleChromosome.of(1, 10),
        // Sigma Space
        DoubleChromosome.of(1, 10),
        // Kernel Size
        DoubleChromosome.of(3, 5),
        // Sure Foreground
        DoubleChromosome.of(0, 255),
        // Sure Background Fraction
        DoubleChromosome.of(0, 0.9),
        // Blob neighbourhood width
        DoubleChromosome.of(1, 15),
        // Blob neighbourhood height
        DoubleChromosome.of(1, 15),
        // Blob neighbourhood depth
        DoubleChromosome.of(1, 15),
        // DOG threshold
        DoubleChromosome.of(1, 255),
        // Gradient threshold
        DoubleChromosome.of(1, 255),
        // Num sigma values
        DoubleChromosome.of(2, 15));
  }

  /**
   * @param args optionally can provide an integer that is used as the reading number
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Create optimiser
    int popSize = ConfigHelper.getInt(SegOpt.POPULATION);
    int generations = ConfigHelper.getInt(SegOpt.GENERATIONS);
    int numStacks = ConfigHelper.getInt(SegOpt.STACKS);
    SegmentationOptimiser optimiser = new SegmentationOptimiser(popSize, generations, numStacks);

    // Load the persisted population if configured to
    if (ConfigHelper.getBoolean(SegOpt.LOAD_POPULATION)) {
      optimiser.loadPopulation();
    }

    // Run the optimiser
    optimiser.run();
  }

}
