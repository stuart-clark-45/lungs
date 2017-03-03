package config;

import org.opencv.core.Mat;

import core.Lungs;

/**
 * The keys for configuration variables that relate to {@link Lungs#extractRois(Mat)} (List)}.
 *
 * @author Stuart Clark
 */
public class Segmentation {

  private Segmentation() {}

  public static class Filter {
    private Filter() {}

    public static final String KERNEL_SIZE = "segmentation.filter.kernelsize";
    public static final String SIGMA_COLOUR = "segmentation.filter.sigmacolor";
    public static final String SIGMA_SPACE = "segmentation.filter.sigmaspace";
  }

  public static final String SURE_FG = "segmentation.surefg";
  public static final String SURE_BG = "segmentation.surebg";
  public static final String EROSION_SIZE = "segmentation.erosion";

}
