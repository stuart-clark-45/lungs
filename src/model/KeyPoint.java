package model;

import org.opencv.core.Point;

/**
 * Created by stuart on 20/03/2017.
 */
public class KeyPoint {

  private final Point point;

  private final double sigma;

  private final double intensity;

  public KeyPoint(Point point, double sigma, double intensity) {
    this.point = point;
    this.sigma = sigma;
    this.intensity = intensity;
  }

  public Point getPoint() {
    return point;
  }

  public double getSigma() {
    return sigma;
  }

  public double getIntensity() {
    return intensity;
  }

}
