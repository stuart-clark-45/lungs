package model.roi;

import java.util.Iterator;

import org.mongodb.morphia.annotations.Transient;

/**
 * Used to hold and access values from a histogram.
 *
 * @author Stuart Clark
 */
public class Histogram implements Iterator<Double> {

  /**
   * The values in each of the histogram bins. Could be frequencies or summations.
   */
  private double[] vals;
  @Transient
  private int index;

  public Histogram(double[] vals) {
    this.vals = vals;
  }

  public double[] getVals() {
    return vals;
  }

  /**
   * Reset the index counter so when {@link Histogram#next()} is next called the first element value
   * from the histogram will be returned.
   */
  public void reset() {
    index = 0;
  }

  @Override
  public boolean hasNext() {
    return index < vals.length;
  }

  @Override
  public Double next() {
    return vals[index++];
  }

}
