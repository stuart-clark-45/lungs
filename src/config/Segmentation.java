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

    public static final String SIZE = "segmentation.filter.size";
    public static final String SIGMA_COLOUR = "segmentation.filter.sigmacolor";
    public static final String SIGMA_SPACE = "segmentation.filter.sigmaspace";
  }

  public static class Threshold {
    private Threshold() {}

    public static final String VAL = "segmentation.threshold.val";
    public static final String METHOD = "segmentation.threshold.method";
    public static final String SIZE = "segmentation.threshold.size";
    public static final String C = "segmentation.threshold.c";
  }

  public static class Opening {
    private Opening() {}

    public static final String KERNEL = "segmentation.opening.kernel";
    public static final String WIDTH = "segmentation.opening.width";
    public static final String HEIGHT = "segmentation.opening.height";
  }

}
