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

  @Transient
  private int index;

  /**
   * The number of possible values
   */
  private int numPosVal;

  private Histogram() {
    // For morphia
  }

  public Histogram(int numPosVal) {
    this.numPosVal = numPosVal;
  }

  public void setBins(double[] bins) {
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

  /**
   * @param mat the single channel {@link Mat} to create the histogram for.
   * @param numBins the number of bins that should be used in the {@link Histogram} returned. should
   *        be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   * @throws LungsException if parameters given are invalid.
   */
  public void createHist(Mat mat, int numBins) throws LungsException {
    validateParams(mat, numBins);

    // Count up the number of occurrences for each value
    double[] valCount = new double[numPosVal];
    for (int row = 0; row < mat.rows(); row++) {
      for (int col = 0; col < mat.cols(); col++) {
        int val = (int) mat.get(row, col)[0];
        valCount[val]++;
      }
    }

    createHist(valCount, mat.rows() * mat.cols(), numBins);
  }

  /**
   * @param region the region of interest that the histogram should be calculated for. i.e. should
   *        be a list of all the points that belong to the region of interest.
   * @param mat the single channel {@link Mat} to create the histogram for.
   * @param numBins the number of bins that should be used in the {@link Histogram} returned. should
   *        be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   * @throws LungsException if parameters given are invalid.
   */
  public void createHist(List<Point> region, Mat mat, int numBins) throws LungsException {
    validateParams(mat, numBins);

    // Count up the number of occurrences for each value
    double[] valCount = new double[numPosVal];
    for (Point point : region) {
      int val = (int) get(mat, point)[0];
      valCount[val]++;
    }

    createHist(valCount, region.size(), numBins);
  }

  /**
   * @param valCount bins that hold counts for the number of pixels with intensity values that
   *        correspond to the bin index.
   * @param numPixels the total number of pixels that have been added to {@code valCount}
   * @param numBins the number of bins used in the {@link Histogram} that will be created.
   */
  private void createHist(double[] valCount, int numPixels, int numBins) {
    // Create a histogram with the correct number of bins
    bins = new double[numBins];
    int binIndex = 0;
    int valPerBin = (int) Math.ceil(numPosVal / numBins);
    int counter = 0;
    for (double val : valCount) {
      // Add val to the current value in the bin
      bins[binIndex] += val;

      // Move onto the next bin if required
      if (counter != 0 && counter % valPerBin == 0) {
        binIndex++;
      }

      counter++;
    }

    // Covert to frequencies
    for (int i = 0; i < bins.length; i++) {
      bins[i] /= numPixels;
    }

    reset();
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
