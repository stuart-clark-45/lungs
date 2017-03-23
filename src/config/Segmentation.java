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

  public static class Blob {
    private Blob() {}

    public static final String NEIGHBOURHOOD_WIDTH = "segmentation.blob.neighbourhoodWidth";
    public static final String NEIGHBOURHOOD_HEIGHT = "segmentation.blob.neighbourhoodHeight";
    public static final String NEIGHBOURHOOD_DEPTH = "segmentation.blob.neighbourhoodDepth";
    public static final String DOG_THRESH = "segmentation.blob.dogThresh";
    public static final String GRADIENT_THRESH = "segmentation.blob.gradientThresh";
    public static final String NUM_SIGMA = "segmentation.blob.numSigma";
    public static final String SURE_FG = "segmentation.blob.surefg";
    public static final String SURE_BG = "segmentation.blob.surebg";
  }

  public static final String SURE_FG = "segmentation.surefg";
  public static final String SURE_BG = "segmentation.surebg";

}
