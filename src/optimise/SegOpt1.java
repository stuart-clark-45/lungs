package optimise;


import java.io.IOException;

import org.jenetics.DoubleChromosome;
import org.jenetics.DoubleGene;
import org.jenetics.Genotype;
import org.jenetics.util.Factory;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.SegOpt;
import core.Lungs;
import util.ConfigHelper;
import vision.BilateralFilter;
import vision.BlobDetector;
import vision.ROIExtractor;

/**
 * Used to optimise the parameters that are used to segment only focuses of maximising the
 * segmentation of true positives.
 *
 * @author Stuart Clark
 */
public class SegOpt1 extends Optimiser<DoubleGene, Double> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SegOpt1.class);

  /*
   * Indexes in genotype for parameters to be optimised
   */
  private static int I = 0;
  private static final int SIGMA_COLOUR = I++;
  private static final int SIGMA_SPACE = I++;
  private static final int KERNEL_SIZE = I++;
  private static final int SURE_FG = I++;
  private static final int SURE_BG_FRAC = I++;

  protected final LungsOptHelper helper;

  /**
   * @param generations the maximum number of generations that should be used.
   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
   */
  public SegOpt1(int popSize, int generations, int numStacks) {
    super(popSize, generations);
    helper = new LungsOptHelper(numStacks);
    LOGGER.info(helper.getMats().size() + " Mats will be used in eval(..)");
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

    // Create the blob detector (it is not actually used as lungs.juxtapleural is set to false)
    BlobDetector detector = new BlobDetector(1, 1);

    // Segment the Mats
    Lungs lungs = new Lungs(filter, extractor, detector);
    lungs.setJuxtapleural(false);

    return calcFitness(lungs);
  }

  protected double calcFitness(Lungs lungs) {
    return helper.noduleInclusion(lungs);
  }

  @Override
  protected String gtToString(Genotype<DoubleGene> gt) {
    return "\n# Size of the kernel used by the bilateral filter\n"
        + "segmentation.filter.kernelsize = " + getDouble(gt, KERNEL_SIZE) + "\n"
        + "# Sigma for colour used by the bilateral filter\n" + "segmentation.filter.sigmacolor = "
        + getDouble(gt, SIGMA_COLOUR) + "\n" + "# Sigma for space used by the bilateral filter\n"
        + "segmentation.filter.sigmaspace = " + getDouble(gt, SIGMA_SPACE) + "\n"
        + "# The threshold used to obtain the sure foreground\n" + "segmentation.surefg = "
        + getDouble(gt, SURE_FG) + "\n" + "# The threshold used to obtain the sure background\n"
        + "segmentation.surebgFraction = " + getDouble(gt, SURE_BG_FRAC) + "\n";
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
        DoubleChromosome.of(0, 0.9));
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Create optimiser
    int popSize = ConfigHelper.getInt(SegOpt.POPULATION);
    int generations = ConfigHelper.getInt(SegOpt.GENERATIONS);
    int numStacks = ConfigHelper.getInt(SegOpt.STACKS);
    SegOpt1 optimiser = new SegOpt1(popSize, generations, numStacks);

    // Load the persisted population if configured to
    if (ConfigHelper.getBoolean(SegOpt.LOAD_POPULATION)) {
      optimiser.loadPopulation();
    }

    // Run the optimiser
    optimiser.run();
  }

}
