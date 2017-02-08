package optimise;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.Population;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
import org.jenetics.util.IO;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FindOptions;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.LungsException;
import util.MongoHelper;
import vision.Matcher;
import vision.ROIExtractor;

/**
 * Used to optimise the parameters that are used to segment
 *
 * @author Stuart Clark
 */
public class SegmentationOptimiser {

  private static final Logger LOGGER = LoggerFactory.getLogger(SegmentationOptimiser.class);
  private static final String POPULATION_FILE = "seg-opt-population.xml";

  /*
   * Indexes in genotype for parameters to be optimised
   */
  private static int I = 0;
  private static final int SIGMA_COLOUR = I++;
  private static final int SIGMA_SPACE = I++;
  private static final int KERNEL_SIZE = I++;
  private static final int THRESHOLD_VAL = I++;
  private static final int THRESHOLD_METHOD = I++;
  private static final int THRESHOLD_SIZE = I++;
  private static final int THRESHOLD_C = I++;
  private static final int OPENING_WIDTH = I++;
  private static final int OPENING_HEIGHT = I++;
  private static final int OPENING_KERNEL = I++;

  /**
   * The maximum number of generations that should be used.
   */
  private final int generations;

  /**
   * the maximum number of times deltaFitness can be 0 before the GA is stopped.
   */
  private final int stagnationLimit;

  /**
   * The GA engine.
   */
  private Engine<IntegerGene, Double> engine;

  /**
   * The {@link Mat}s to segment.
   */
  private List<Mat> mats;

  /**
   * The points list of {@link Point}s that must be included in the segmentation.
   */
  private List<List<GroundTruth>> groundTruths;

  /**
   * The current population.
   */
  private Population<IntegerGene, Double> population;

  /**
   * The total number of {@link GroundTruth}s used in the fitness function.
   */
  private int totalGTs;

  /**
   * @param generations the maximum number of generations that should be used.
   * @param stagnationLimit the maximum number of times deltaFitness can be 0 before the GA is
   *        stopped.
   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
   * @param readingNumber The reading number that should be used when selecting ground truths. See
   *        documentation at {@link GroundTruth#readingNumber}.
   */
  public SegmentationOptimiser(int generations, int stagnationLimit, int numStacks,
      int readingNumber) {
    this.generations = generations;
    this.stagnationLimit = stagnationLimit;
    this.mats = new ArrayList<>();
    this.groundTruths = new ArrayList<>();

    // Load some stacks
    Datastore ds = MongoHelper.getDataStore();
    List<CTStack> stacks =
        ds.createQuery(CTStack.class).field("model").equal("Sensation 16")
            .asList(new FindOptions().limit(numStacks));

    // For each slice in all the stacks
    for (CTStack stack : stacks) {
      for (CTSlice slice : stack.getSlices()) {

        // Find all the first readings for the slice that contain a nodule. Only the first reading
        // is used as we need to know exactingly how many nodules there are in the set of Mats we
        // will use
        List<GroundTruth> gtList =
            ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                .field("imageSopUID").equal(slice.getImageSopUID()).field("readingNumber")
                .equal(readingNumber).asList();

        // If there are nodules in the slice
        if (!gtList.isEmpty()) {

          // Add Mat for slice into list that will be used in eval(..)
          mats.add(Lungs.getSliceMat(slice));

          // Add gtLists to list that will be used in eval(..)
          groundTruths.add(gtList);
          totalGTs += gtList.size();
        }

      }
    }

    LOGGER.info(mats.size() + " Mats will be used in eval(..)");

    // Define the genotype factory
    Factory<Genotype<IntegerGene>> gtf = Genotype.of(
    // Sigma Colour
        IntegerChromosome.of(1, 10),
        // Sigma Space
        IntegerChromosome.of(1, 10),
        // Kernel Size
        IntegerChromosome.of(3, 9),
        // Threshold Value
        IntegerChromosome.of(0, 255),
        // Threshold Method
        IntegerChromosome.of(-1, 1),
        // Adaptive Threshold Size
        IntegerChromosome.of(3, 9),
        // Adaptive Threshold C
        IntegerChromosome.of(-255, 255),
        // Opening Width
        IntegerChromosome.of(1, 10),
        // Opening Height
        IntegerChromosome.of(1, 10),
        // Opening Kernel
        IntegerChromosome.of(1, 2));

    // Create the execution environment
    this.engine = Engine.builder(this::eval, gtf).build();

    LOGGER.info("Population size of: " + this.engine.getPopulationSize());
  }

  /**
   * Run the GA and print the results
   */
  @SuppressWarnings("ConstantConditions")
  public void run() {
    LOGGER.info("Running SegmentationOptimiser...");

    // Load population or create new one
    Iterator<EvolutionResult<IntegerGene, Double>> iterator;
    if (population != null) {
      iterator = engine.iterator(population);
    } else {
      iterator = engine.iterator();
    }

    // Hold the fitness of the previous generation
    double lastFitness = 0.0;
    // Used to count the number of generations that deltaFitness has been 0.0
    int stagnation = 0;
    // Used to count the number of generations
    int counter = 0;

    // Run the GA
    while (stagnation < stagnationLimit && ++counter <= generations) {
      // Process generation
      EvolutionResult<IntegerGene, Double> result = iterator.next();
      population = result.getPopulation();

      // Check if stagnating
      Double fitness = result.getBestFitness();
      double deltaFitness = fitness - lastFitness;
      if (deltaFitness == 0.0) {
        stagnation++;
      } else {
        stagnation = 0;
      }
      lastFitness = fitness;

      // Logging
      LOGGER.info("Generation " + counter + "/" + generations + " complete with best fitness of: "
          + fitness + " delta fitness of: " + deltaFitness);
      Genotype<IntegerGene> gt = result.getBestPhenotype().getGenotype();

      String s =
          "\n+" + "# Size of the kernel used by the bilateral filter\n"
              + "segmentation.filter.size = "
              + getInt(gt, KERNEL_SIZE)
              + "\n"
              + "# Sigma for colour used by the bilateral filter\n"
              + "segmentation.filter.sigmacolor = "
              + getInt(gt, SIGMA_COLOUR)
              + "\n"
              + "# Sigma for space used by the bilateral filter\n"
              + "segmentation.filter.sigmaspace = "
              + getInt(gt, SIGMA_SPACE)
              + "\n"
              + "# The threshold value used\n"
              + "segmentation.threshold.val = "
              + getInt(gt, THRESHOLD_VAL)
              + "\n"
              + "# Addaptive thresholding method: -1 = NONE, ADAPTIVE_THRESH_MEAN_C = 0, ADAPTIVE_THRESH_GAUSSIAN_C = 1\n"
              + "segmentation.threshold.method = "
              + getInt(gt, THRESHOLD_METHOD)
              + "\n"
              + "# Neighbour hood size for addaptive thresholding must be 3, 5, 7 ...\n"
              + "segmentation.threshold.size = "
              + getThresholdSize(gt)
              + "\n"
              + "# Constant subtracted from the mean or weighted mean when using adative thresholding\n"
              + "segmentation.threshold.c = "
              + getInt(gt, THRESHOLD_C)
              + "\n"
              + "# The type of kernel to use: MORPH_RECT = 0, MORPH_CROSS = 1, MORPH_ELLIPSE = 2\n"
              + "segmentation.opening.kernel = "
              + getInt(gt, OPENING_KERNEL)
              + "\n"
              + "# The width of the kernel to use\n"
              + "segmentation.opening.width = "
              + getInt(gt, OPENING_WIDTH)
              + "\n"
              + "# The height of the kernel to use\n"
              + "segmentation.opening.height = " + getInt(gt, OPENING_HEIGHT);

      LOGGER.info(s);
    }

    LOGGER.info("SegmentationOptimiser Finished");
    Toolkit.getDefaultToolkit().beep();
  }

  /**
   * Determine the fitness of {@code gt} by calculating how accurately is segments the
   *
   * @param gt
   * @return the fitness of {@code gt}.
   */
  private Double eval(Genotype<IntegerGene> gt) {
    // Segment the Mats
    Lungs lungs =
        new Lungs(getInt(gt, SIGMA_COLOUR), getInt(gt, SIGMA_SPACE), getInt(gt, KERNEL_SIZE),
            getInt(gt, THRESHOLD_VAL), getInt(gt, THRESHOLD_METHOD), getThresholdSize(gt), getInt(
                gt, THRESHOLD_C), getInt(gt, OPENING_WIDTH), getInt(gt, OPENING_HEIGHT), getInt(gt,
                OPENING_KERNEL));
    List<Mat> segmented = lungs.segment(mats);

    // Extract the ROIs for the mats
    ROIExtractor extractor = new ROIExtractor(Lungs.FOREGROUND);
    // Each sublist contains all the ROIs for the corresponding Mat in segmented
    List<List<ROI>> allROIs = new ArrayList<>();
    int numROIs = 0;
    for (Mat mat : segmented) {
      List<ROI> rois;
      try {
        rois = extractor.extract(mat);
        allROIs.add(rois);
        numROIs += rois.size();
      } catch (LungsException e) {
        throw new IllegalStateException("Failed to extract ROIs", e);
      }
    }

    double inclusion = noduleInclusion(allROIs);

    double fitness = inclusion;
    if (inclusion > 0.8) {
      fitness += Double.MAX_VALUE / 2;
      fitness -= numROIs;
    }

    return fitness;
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

  /**
   * @param gt
   * @return the value for the threshold size chromosome. (With one added if it is even)
   */
  private int getThresholdSize(Genotype<IntegerGene> gt) {
    int val = getInt(gt, THRESHOLD_SIZE);
    if(val % 2 == 0){
      val++;
    }
    return val;
  }

  /**
   * @param gt
   * @param index
   * @return the integer value for the chromosome at {@code index}.
   */
  private int getInt(Genotype<IntegerGene> gt, int index) {
    return gt.getChromosome(index).getGene().intValue();
  }

  public void savePopulation() {
    try {
      IO.jaxb.write(population, new File(POPULATION_FILE));
      LOGGER.info("Saved population to " + POPULATION_FILE);
    } catch (IOException e) {
      LOGGER.error("Failed to write population to " + POPULATION_FILE);
    }
  }

  @SuppressWarnings("unchecked")
  public void loadPopulation() throws IOException {
    final File file = new File(POPULATION_FILE);
    if (file.exists()) {
      this.population = (Population<IntegerGene, Double>) IO.jaxb.read(file);
      LOGGER.info("Loaded population from " + POPULATION_FILE);
    } else {
      LOGGER.info("There is no population to load will generate a new one");
    }
  }

  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Create optimiser
    int generations = 20000;
    int stagnationLimit = generations + 1;
    // int numStacks = 10;
    int numStacks = 1;
    int readingNumber = 0;
    SegmentationOptimiser optimiser =
        new SegmentationOptimiser(generations, stagnationLimit, numStacks, readingNumber);

    // Load the persisted population if there is one
    // optimiser.loadPopulation();

    // Save population if interrupted
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOGGER.info("Executing shutdown hook");
      optimiser.savePopulation();
    }));

    // Run the optimiser
    optimiser.run();
  }

}
