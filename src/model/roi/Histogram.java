package model.roi;

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

  private static final int NUM_POSSIBLE_VALS = 256;

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

  /**
   * @param mat the single channel {@link Mat} to create the histogram for.
   * @param numBins the number of bins that should be used in the {@link Histogram} returned. should
   *        be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   * @return
   * @throws LungsException if parameters given are invalid.
   */
  public static Histogram createHist(Mat mat, int numBins) throws LungsException {
    validateParams(mat, numBins);

    // Count up the number of occurrences for each value
    double[] valCount = new double[NUM_POSSIBLE_VALS];
    for (int row = 0; row < mat.rows(); row++) {
      for (int col = 0; col < mat.cols(); col++) {
        int val = (int) mat.get(row, col)[0];
        valCount[val]++;
      }
    }

    return createHist(valCount, mat.rows() * mat.cols(), numBins);
  }

  /**
   * @param roi the {@link ROI} that the histogram should be calculated for.
   * @param mat the single channel {@link Mat} to create the histogram for.
   * @param numBins the number of bins that should be used in the {@link Histogram} returned. should
   *        be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   * @return
   * @throws LungsException if parameters given are invalid.
   */
  public static Histogram createHist(ROI roi, Mat mat, int numBins) throws LungsException {
    validateParams(mat, numBins);

    List<Point> points = roi.getRegion();

    // Count up the number of occurrences for each value
    double[] valCount = new double[NUM_POSSIBLE_VALS];
    for (Point point : roi.getRegion()) {
      int val = (int) mat.get((int) point.y, (int) point.x)[0];
      valCount[val]++;
    }

    return createHist(valCount, points.size(), numBins);
  }

  /**
   *
   * @param valCount bins that hold counts for the number of pixels with intensity values that
   *        correspond to the bin index.
   * @param nubPixels the total number of pixels that have been added to {@code valCount}
   * @param numBins the number of bins used in the {@link Histogram} that will be created.
   * @return A histogram which has frequencies as it's bin values as appose to counts.
   */
  private static Histogram createHist(double[] valCount, int nubPixels, int numBins) {
    // Create a histogram with the correct number of bins
    double[] hist = new double[numBins];
    int binIndex = 0;
    int valPerBin = (int) Math.ceil(NUM_POSSIBLE_VALS / numBins);
    int counter = 0;
    for (double val : valCount) {
      // Add val to the current value in the bin
      hist[binIndex] += val;

      // Move onto the next bin if required
      if (counter != 0 && counter % valPerBin == 0) {
        binIndex++;
      }

      counter++;
    }

    // Covert to frequencies
    for (int i = 0; i < hist.length; i++) {
      hist[i] /= nubPixels;
    }

    return new Histogram(hist);
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
