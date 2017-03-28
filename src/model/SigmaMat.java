package model;

import org.opencv.core.Mat;

/**
 * Simple model used to hold a {@link Mat} and it's corresponding sigma value.
 *
 * @author Stuart Clark
 */
public class SigmaMat {

  private final double sigma;
  private final Mat mat;

  public SigmaMat(Mat mat, double sigma) {
    this.mat = mat;
    this.sigma = sigma;
  }

  public double getSigma() {
    return sigma;
  }

  public Mat getMat() {
    return mat;
  }

}
