package model;

import static util.MatUtils.get;

import java.util.Iterator;
import java.util.List;

import org.mongodb.morphia.annotations.Transient;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import util.LungsException;

/**
 * Used to hold and access values from a histogram.
 *
 * @author Stuart Clark
 */
public class Histogram implements Iterator<Double> {

  /**
   * The number of possible values in an 8 Bit image
   */
  public static final int POS_VALS_8BIT = 256;

  /**
   * The values in each of the histogram bins. Could be frequencies or summations.
   */
  private double[] bins;

  /**
   * The number of bins in the histogram.
   */
  private int numBins;

  /**
   * Array used to count values.
   */
  @Transient
  private double[] valCounter;

  /**
   * Used to count the total number of values
   */
  private double totalCounter;

  @Transient
  private int index;

  /**
   * The number of possible values
   */
  private int numPosVal;

  private Histogram() {
    // For morphia
  }

  /**
   * @param numBins the number of bins that should be used in the {@link Histogram} returned. should
   *        be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   * @param numPosVal the number of possible values that can be stored in the histogram.
   */
  public Histogram(int numBins, int numPosVal) {
    this.numBins = numBins;
    this.numPosVal = numPosVal;
    this.valCounter = new double[numPosVal];
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

  public void setBins(double[] bins) {
    this.bins = bins;
  }

  public double[] getBins() {
    return bins;
  }

  public int getNumBins() {
    return numBins;
  }

  public void add(int val) {
    valCounter[val]++;
    totalCounter++;
  }

  /**
   * @param mat the single channel {@link Mat} to create the histogram for.
   * @throws LungsException if parameters given are invalid.
   */
  public void add(Mat mat) throws LungsException {
    validateParams(mat, numBins);

    // Count up the number of occurrences for each value
    valCounter = new double[numPosVal];
    for (int row = 0; row < mat.rows(); row++) {
      for (int col = 0; col < mat.cols(); col++) {
        int val = (int) mat.get(row, col)[0];
        valCounter[val]++;
      }
    }

    totalCounter += mat.rows() * mat.cols();
  }

  /**
   * @param region the region of interest that the histogram should be calculated for. i.e. should
   *        be a list of all the points that belong to the region of interest.
   * @param mat the single channel {@link Mat} to create the histogram for.
   * 
   * @throws LungsException if parameters given are invalid.
   */
  public void add(List<Point> region, Mat mat) throws LungsException {
    validateParams(mat, numBins);

    // Count up the number of occurrences for each value
    valCounter = new double[numPosVal];
    for (Point point : region) {
      int val = (int) get(mat, point)[0];
      valCounter[val]++;
    }

    totalCounter += region.size();
  }

  /**
   * Take the values added to the Histogram and place them into bins.
   */
  public void computeBins() {
    // Create a histogram with the correct number of bins
    bins = new double[numBins];
    int binIndex = 0;
    int valPerBin = (int) Math.ceil(numPosVal / numBins);
    int counter = 0;
    for (double val : valCounter) {
      // Add val to the current value in the bin
      bins[binIndex] += val;

      // Move onto the next bin if required
      if (counter != 0 && counter % valPerBin == 0) {
        binIndex++;
      }

      counter++;
    }

  }

  /**
   * Convert the value in the bins to frequencies rather than counts
   */
  public void toFrequencies() {
    for (int i = 0; i < bins.length; i++) {
      bins[i] /= totalCounter;
    }
  }

  /**
   * Checks that {@code mat} has one channel and that {@code numBins} is a power of 2.
   * 
   * @param mat
   * @param numBins
   * @throws LungsException
   */
  private static void validateParams(Mat mat, int numBins) throws LungsException {
    if (mat.channels() != 1) {
      throw new LungsException("mat must have 1 channel");
    }

    if ((numBins & -numBins) != numBins) {
      throw new LungsException("numBins must be a power of two but is " + numBins);
    }
  }

}
