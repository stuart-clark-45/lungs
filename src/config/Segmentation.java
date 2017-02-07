package config;

import java.util.List;

import core.Lungs;

/**
 * The keys for configuration variables that relate to {@link Lungs#segment(List)}.
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

  public static final String THRESHOLD = "segmentation.threshold";

  public static class Opening {
    private Opening() {}

    public static final String KERNEL = "segmentation.opening.kernel";
    public static final String WIDTH = "segmentation.opening.width";
    public static final String HEIGHT = "segmentation.opening.height";
  }

}
