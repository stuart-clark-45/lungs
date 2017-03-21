package vision;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import util.MatUtils;

/**
 * Used to hold the parameters for and apply a bilateral filter to {@link Mat}s.
 *
 * @author Stuart Clark
 */
public class BilateralFilter {

  /**
   * The size of the kernel that should be used for the filter.
   */
  private final int kernelSize;

  /**
   * The sigma in the colour space. A larger value of the parameter means that farther colours
   * within the pixel neighborhood will be mixed together, resulting in larger areas of semi-equal
   * colour.
   */
  private final int sigmaColour;

  /**
   * The sigma in the coordinate space. A larger value of the parameter means that farther pixels
   * will influence each other as long as their colours are close enough see {@code sigmaColour}.
   */
  private final int sigmaSpace;

  /**
   * @param kernelSize the size of the kernel that should be used for the filter.
   * @param sigmaColour the sigma in the colour space. A larger value of the parameter means that
   *        farther colours within the pixel neighborhood will be mixed together, resulting in
   *        larger areas of semi-equal colour.
   * @param sigmaSpace the sigma in the coordinate space. A larger value of the parameter means that
   *        farther pixels will influence each other as long as their colours are close enough see
   *        {@code sigmaColour}.
   */
  public BilateralFilter(int kernelSize, int sigmaColour, int sigmaSpace) {
    this.kernelSize = kernelSize;
    this.sigmaColour = sigmaColour;
    this.sigmaSpace = sigmaSpace;
  }

  /**
   * @param original
   * @return {@code original} with a bilateral filter applied using the parameters given in the
   *         constructor.
   */
  public Mat filter(Mat original) {
    Mat filtered = MatUtils.similarMat(original);
    Imgproc.bilateralFilter(original, filtered, kernelSize, sigmaColour, sigmaSpace);
    return filtered;
  }

}
