package model;

import org.opencv.core.Point;

/**
 * Created by stuart on 20/03/2017.
 */
public class KeyPoint {

  private final Point point;

  private final double radius;

  private final double intensity;

  public KeyPoint(Point point, double radius, double intensity) {
    this.point = point;
    this.radius = radius;
    this.intensity = intensity;
  }

  public Point getPoint() {
    return point;
  }

  public double getRadius() {
    return radius;
  }

  public double getIntensity() {
    return intensity;
  }

}
