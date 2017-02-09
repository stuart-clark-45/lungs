package model;

/**
 * Simple model used to hold minimum and maximum x and y value.
 *
 * @author Stuart Clark
 */
public class MinMaxXY<T> {

  private T minX;
  private T maxX;
  private T minY;
  private T maxY;

  public T getMinX() {
    return minX;
  }

  public void setMinX(T minX) {
    this.minX = minX;
  }

  public T getMaxX() {
    return maxX;
  }

  public void setMaxX(T maxX) {
    this.maxX = maxX;
  }

  public T getMinY() {
    return minY;
  }

  public void setMinY(T minY) {
    this.minY = minY;
  }

  public T getMaxY() {
    return maxY;
  }

  public void setMaxY(T maxY) {
    this.maxY = maxY;
  }

}
