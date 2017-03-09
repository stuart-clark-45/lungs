package util;

import static java.lang.Short.MAX_VALUE;

/**
 * Used for obtaining commonly used colours in BGR (blue, green, reg colour space).
 *
 * @author Stuart Clark
 */
public class ColourBGR {

  public static final double[] WHITE = new double[] {MAX_VALUE, MAX_VALUE, MAX_VALUE};
  public static final double[] RED = new double[] {0, 0, MAX_VALUE};
  public static final double[] GREEN = new double[] {0, MAX_VALUE, 0};
  public static final double[] BLUE = new double[] {MAX_VALUE, 0, 0};
  public static final double[] ORANGE = new double[] {0, MAX_VALUE / 2, MAX_VALUE};

  private ColourBGR() {
    // Hide constructor
  }

}
