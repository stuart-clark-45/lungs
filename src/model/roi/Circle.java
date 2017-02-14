package model.roi;

import org.opencv.core.Point;

/**
 * @author Stuart Clark used to hold enough information to draw a circle.
 */
public class Circle {

  private Point center;
  private double radius;

  public Point getCenter() {
    return center;
  }

  public void setCenter(Point center) {
    this.center = center;
  }

  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }

}
