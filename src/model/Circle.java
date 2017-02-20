package model;

import org.opencv.core.Point;

/**
 * Used to hold enough information to draw a circle.
 *
 * @author Stuart Clark
 */
public class Circle {

  private Point center;
  private double radius;

  private Circle() {
    // For morphia
  }

  public Circle(Point center, double radius) {
    this.center = center;
    this.radius = radius;
  }

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
