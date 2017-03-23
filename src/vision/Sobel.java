package vision;

import static org.opencv.core.CvType.CV_16S;
import static org.opencv.core.CvType.CV_32FC1;
import static util.MatUtils.get;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

/**
 * Used to perform Sobel edge detection on a {@link Mat}.
 *
 * @author Stuart Clark
 */
public class Sobel {

  private Mat mat;
  private Mat deltaX;
  private Mat deltaY;

  public Sobel(Mat mat) {
    this.mat = mat;

    // Delta X
    deltaX = new Mat(mat.rows(), mat.cols(), CV_16S);
    Imgproc.Sobel(mat, deltaX, CV_16S, 1, 0);

    // Delta Y
    deltaY = new Mat(mat.rows(), mat.cols(), CV_16S);
    Imgproc.Sobel(mat, deltaY, CV_16S, 0, 1);
  }

  /**
   * @return a {@link Mat} containing the gradient magnitude for each pixel in {@code mat}.
   */
  public Mat magnitude() {
    // Delta X squared
    Mat gradXPow2 = new Mat(mat.rows(), mat.cols(), CV_16S);
    Core.pow(deltaX, 2, gradXPow2);

    // Delta Y squared
    Mat gradYPow2 = new Mat(mat.rows(), mat.cols(), CV_16S);
    Core.pow(deltaY, 2, gradYPow2);

    // Calculate gradient magnitude
    Mat sum = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    Core.add(gradXPow2, gradYPow2, sum);
    Mat sumFloat = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    sum.convertTo(sumFloat, sumFloat.type());
    Mat gradientMag = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    Core.sqrt(sumFloat, gradientMag);

    return gradientMag;
  }

  /**
   * @param point
   * @return the gradient orientation at the {@code point} in radians.
   */
  public double orientation(Point point) {
    return Math.atan(get(deltaY, point)[0] / get(deltaX, point)[0]);
  }

  public Mat getDeltaX() {
    return deltaX;
  }

  public Mat getDeltaY() {
    return deltaY;
  }

}
