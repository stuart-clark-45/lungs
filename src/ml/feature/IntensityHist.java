package ml.feature;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.roi.Histogram;
import model.roi.ROI;
import util.LungsException;

/**
 * Abstract class used to create histograms features.
 *
 * @author Stuart Clark
 */
public abstract class IntensityHist implements Feature {

  private static final int NUM_POSSIBLE_VALS = 256;

  /**
   * The number of numBins that should be used for the histogram.
   */
  private int numBins;

  /**
   * The {@link Histogram} computed using {@code this}.
   */
  private Histogram histogram;

  /**
   * @param numBins should be a power of two e.g. 2, 4, 8, 16, 32, 64, 128, 256
   */
  public IntensityHist(int numBins) {
    if ((numBins & -numBins) == numBins) {
      this.numBins = numBins;
    } else {
      throw new IllegalArgumentException("numBins must be a power of two but is " + numBins);
    }
  }

  protected void createHist(ROI roi, Mat mat) throws LungsException {
    if (mat.channels() != 1) {
      throw new LungsException("mat must have 1 channel");
    }

    List<Point> points = roi.getRegion();

    // Count up the number of occurrences for each value
    double[] valCount = new double[NUM_POSSIBLE_VALS];
    for (Point point : roi.getRegion()) {
      int val = (int) mat.get((int) point.y, (int) point.x)[0];
      valCount[val]++;
    }

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
      hist[i] /= points.size();
    }

    histogram = new Histogram(hist);
  }

  public Histogram getHistogram() {
    return histogram;
  }
}
