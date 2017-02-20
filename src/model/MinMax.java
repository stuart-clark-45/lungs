package model;

/**
 * Used to hold a pair of minimum and maximum values.
 *
 * @author Stuart Clark
 */
public class MinMax<T extends Number> {

  private final T min;
  private final T max;

  public MinMax(T min, T max) {
    this.min = min;
    this.max = max;
  }

  public T getMin() {
    return min;
  }

  public T getMax() {
    return max;
  }

}
