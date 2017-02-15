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
  private double[] bins;
  @Transient
  private int index;

  private Histogram() {
    // For morphia
  }

  public Histogram(double[] bins) {
    this.bins = bins;
  }

  public double[] getBins() {
    return bins;
  }

  public int numBins() {
    return bins.length;
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
    return index < bins.length;
  }

  @Override
  public Double next() {
    return bins[index++];
  }

}
