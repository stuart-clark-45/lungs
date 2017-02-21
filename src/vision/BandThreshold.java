package vision;

import java.util.List;

import org.opencv.core.Mat;

import model.MinMax;
import util.LungsException;
import util.MatUtils;

/**
 * Used to threshold greyscale {@link Mat}s so that any pixel intensities that fall between the min
 * and max value of the {@code bands} (inclusive) are marked as being part of the foreground and
 * vice versa.
 *
 * @author Stuart Clark
 */
public class BandThreshold {

  /**
   * The colour to assign to the foreground of the thresholded {@link Mat}.
   */
  private final double foreground;

  /**
   * A list of {@link MinMax<Integer>}s. Each one containing the minimum and maximum values
   * (inclusive) of the band of intensities that should be included in the foreground of the
   * thresholded {@link Mat}.
   */
  private final List<MinMax<Integer>> bands;

  /**
   * @param foreground the colour to assign to the foreground of the thresholded {@link Mat}.
   * @param bands {@link MinMax<Integer>}s. Each one containing the minimum and maximum values
   *        (inclusive) of the band of intensities that should be included in the foreground of the
   *        thresholded {@link Mat}.
   */
  public BandThreshold(double foreground, List<MinMax<Integer>> bands) {
    this.foreground = foreground;
    this.bands = bands;
  }

  /**
   * Perform a band threshold on {@code mat}.
   *
   * @param mat the {@link Mat} to threshold.
   * @return the thresholded {@link Mat}.
   * @throws LungsException
   */
  public Mat threshold(Mat mat) throws LungsException {
    if (mat.channels() != 1) {
      throw new LungsException("this method can only currently be used on greyscale Mats");
    }

    Mat thresholded = MatUtils.similarMat(mat);

    // Iterate over the pixels of mat
    for (int row = 0; row < mat.rows(); row++) {
      for (int col = 0; col < mat.cols(); col++) {

        // Get the intensity of the pixel
        double intensity = mat.get(row, col)[0];

        // Check if it lies within any of the bands and set it as the foreground the thresholded Mat
        // if it does
        for (MinMax<Integer> band : bands) {
          if (intensity >= band.getMin() && intensity <= band.getMax()) {
            thresholded.put(row, col, foreground);
            // If is in one band then there is no need to check the others
            break;
          }
        }

      }
    }

    return thresholded;
  }

}
