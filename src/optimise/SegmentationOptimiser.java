package optimise;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
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
  private static final int NUM_STACKS = 1;

  /*
   * Indexes in genotype for parameters to be optimised
   */
  private static final int SIGMA_COLOUR = 0;
  private static final int SIGMA_SPACE = 1;
  private static final int KERNEL_SIZE = 2;
  private static final int THRESHOLD = 3;
  private static final int OPENING_WIDTH = 4;
  private static final int OPENING_HEIGHT = 5;
  private static final int OPENING_KERNEL = 6;

  private final int generations;
  private Engine<IntegerGene, Double> engine;
  private List<Mat> mats;
  private List<List<Point>> groundTruths;

  public SegmentationOptimiser(int generations) {
    this.generations = generations;
    this.mats = new ArrayList<>();
    this.groundTruths = new ArrayList<>();

    // Load some stacks
    Datastore ds = MongoHelper.getDataStore();
    List<CTStack> stacks =
        ds.createQuery(CTStack.class).field("model").equal("Sensation 16")
            .asList(new FindOptions().limit(NUM_STACKS));

    // For each slice in all the stacks
    for (CTStack stack : stacks) {
      for (CTSlice slice : stack.getSlices()) {

        // Find the nodules for the slice
        List<GroundTruth> gtList =
            ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                .field("imageSopUID").equal(slice.getImageSopUID()).asList();

        // If there are nodules in the slice
        if (!gtList.isEmpty()) {

          // Add Mat for slice into list that will be used in eval(..)
          mats.add(Lungs.getSliceMat(slice));

          // Combine the regions into one list of points
          List<Point> combinedGt = new ArrayList<>();
          gtList.stream().map(GroundTruth::getRegion).forEach(combinedGt::addAll);
          groundTruths.add(combinedGt);

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
        IntegerChromosome.of(3, 6),
        // Threshold
        IntegerChromosome.of(0, 255),
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

    Iterator<EvolutionResult<IntegerGene, Double>> iterator = engine.iterator();
    for (int i = 1; i <= generations; i++) {
      // Process generation
      logResult(iterator.next(), i);
    }

    LOGGER.info("SegmentationOptimiser Finished");
  }

  private void logResult(EvolutionResult<IntegerGene, Double> result, int i) {
    LOGGER.info("Generation " + i + "/" + generations + " complete with best fitness of: "
        + result.getBestFitness());
    Genotype<IntegerGene> gt = result.getBestPhenotype().getGenotype();
    String s =
        "Optimised Parameters:\n" + "Sigma Colour: " + getInt(gt, SIGMA_COLOUR) + "\n"
            + "Sigma Space: " + getInt(gt, SIGMA_SPACE) + "\n" + "Filter Kernel Size: "
            + getInt(gt, KERNEL_SIZE) + "\n" + "Threshold: " + getInt(gt, THRESHOLD) + "\n"
            + "Opening Width: " + getInt(gt, OPENING_WIDTH) + "\n" + "Opening Height: "
            + getInt(gt, OPENING_HEIGHT) + "\n" + "Opening Kernel: " + getInt(gt, OPENING_KERNEL)
            + "\n";
    LOGGER.info(s);
  }

  /**
   * Determine the fitness of {@code gt} by calculating how accurately is segments the
   *
   * @param gt
   * @return the fitness of {@code gt}.
   */
  private Double eval(Genotype<IntegerGene> gt) {
    Lungs lungs =
        new Lungs(getInt(gt, SIGMA_COLOUR), getInt(gt, SIGMA_SPACE), getInt(gt, KERNEL_SIZE),
            getInt(gt, THRESHOLD), getInt(gt, OPENING_WIDTH), getInt(gt, OPENING_HEIGHT), getInt(
                gt, OPENING_KERNEL));

    List<Mat> segmented = lungs.segment(mats);

    double fitness = 0.0;
    ROIExtractor extractor = new ROIExtractor(Lungs.FOREGROUND);
    for (int i = 0; i < segmented.size(); i++) {
      // Get ground truth
      List<Point> groundTruth = groundTruths.get(i);

      // Extract ROIs
      List<ROI> rois;
      try {
        rois = extractor.extract(segmented.get(i));
      } catch (LungsException e) {
        throw new IllegalStateException("Failed to extract ROIs", e);
      }

      // Combine ROIs
      List<Point> extractedPoints = new ArrayList<>();
      rois.stream().map(ROI::getPoints).forEach(extractedPoints::addAll);

      // Match ground truth to ROIs
      fitness += Matcher.match(groundTruth, extractedPoints);
    }

    return fitness;
  }

  /**
   * @param gt
   * @param index
   * @return the integer value for the chromosome at {@code index}.
   */
  private int getInt(Genotype<IntegerGene> gt, int index) {
    return gt.getChromosome(index).getGene().intValue();
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new SegmentationOptimiser(200).run();
  }
}
