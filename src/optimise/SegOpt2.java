package optimise;

import java.io.File;
import java.io.IOException;
import java.util.List;

import model.ROI;
import org.opencv.core.Core;

import config.SegOpt;
import util.ConfigHelper;

/**
 * Used to optimise the parameters that are used to segment only focuses of minimising the false
 * positives. {@link SegOpt1} needs to be run before this class is used.
 *
 * @author Stuart Clark
 */
public class SegOpt2 extends SegOpt1 {

  /**
   * The maximum nodule inclusion obtained using {@link SegOpt1}.
   */
  private double maxNoduleInc;

  /**
   * @param popSize
   * @param generations the maximum number of generations that should be used.
   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
   */
  public SegOpt2(double maxNoduleInc, int popSize, int generations, int numStacks) {
    super(popSize, generations, numStacks);
    this.maxNoduleInc = maxNoduleInc;
  }

  @Override
  protected double calcFitness(List<List<ROI>> allROIs) {
    double noduleInc = noduleInclusion(allROIs);

    // If the nodule inclusion has dropped too far then return 0 fitness
    if(Math.abs(maxNoduleInc - noduleInc) > 0.00005){
      return 0;
    }

    int numROIs = allROIs.stream().mapToInt(List::size).sum();

    // Fewer ROIs -> greater fitness
    return -numROIs;
  }

  /**
   * @param args a single argument providing the optimum fitness output by {@link SegOpt1}.
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    double maxNoduleInc;
    try {
      maxNoduleInc = Double.parseDouble(args[0]);
    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("You must provide the max fitness output by SegOpt1");
    }

    // Create optimiser
    int popSize = ConfigHelper.getInt(SegOpt.POPULATION);
    int generations = ConfigHelper.getInt(SegOpt.GENERATIONS);
    int numStacks = ConfigHelper.getInt(SegOpt.STACKS);
    SegOpt2 optimiser = new SegOpt2(maxNoduleInc, popSize, generations, numStacks);

    // Load the persisted population if configured to
    if (!new File(optimiser.populationFile()).exists()) {
      throw new IllegalArgumentException(optimiser.populationFile()
          + " must be in the working directory");
    }
    optimiser.loadPopulation();

    // Run the optimiser
    optimiser.run();
  }

}
