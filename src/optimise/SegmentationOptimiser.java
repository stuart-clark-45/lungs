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
  private static final int SIGMA_COLOUR = 0;
  private static final int SIGMA_SPACE = 1;
  private static final int KERNEL_SIZE = 2;
  private static final int THRESHOLD = 3;
  private static final int OPENING_WIDTH = 4;
  private static final int OPENING_HEIGHT = 5;
  private static final int OPENING_KERNEL = 6;

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
  private List<List<Point>> groundTruths;

  /**
   * The current population.
   */
  private Population<IntegerGene, Double> population;

  /**
   * @param generations the maximum number of generations that should be used.
   * @param stagnationLimit the maximum number of times deltaFitness can be 0 before the GA is
   *        stopped.
   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
   */
  public SegmentationOptimiser(int generations, int stagnationLimit, int numStacks) {
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
          "\n# Size of the kernel used by the bilateral filter\n"
              + "segmentation.filter.kernelsize = "
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
              + "# The threshold used\n"
              + "segmentation.threshold = "
              + getInt(gt, THRESHOLD)
              + "\n"
              + "# The type of kernel to use MORPH_RECT = 0, MORPH_CROSS = 1, MORPH_ELLIPSE = 2\n"
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
    SegmentationOptimiser optimiser = new SegmentationOptimiser(20000, 5, 10);

    // Load the persisted population if there is one
    optimiser.loadPopulation();

    // Save population if interrupted
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOGGER.info("Executing shutdown hook");
      optimiser.savePopulation();
    }));

    // Run the optimiser
    optimiser.run();
  }

}
