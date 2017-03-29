package model;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * Simple model used to hold a {@link Mat} and it's corresponding sigma value.
 *
 * @author Stuart Clark
 */
public class SigmaMat {

  /**
   * The sigma value for the {@code mat}.
   */
  private final double sigma;

  private final Mat mat;

  /**
   * The value used to scale co-ordinates to that they refer to the full size {@link Mat}.
   */
  private double scalar;

  public SigmaMat(Mat mat, double sigma) {
    this.mat = mat;
    this.sigma = sigma;
    this.scalar = 1;
  }

  public Point getScaledPoint(int row, int col) {
    return new Point(scalar * col, scalar * row);
  }

  public void setScalar(double scalar) {
    this.scalar = scalar;
  }

  public double getScalar() {
    return scalar;
  }

  public double getSigma() {
    return sigma;
  }

  public Mat getMat() {
    return mat;
  }

}
