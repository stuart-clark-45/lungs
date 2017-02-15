package ml.feature;

import java.util.List;

import model.roi.Histogram;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.roi.ROI;
import util.LungsException;

/**
 * Computes the intensity histogram for {@link ROI} and updates {@link ROI#intensityHistogram}.
 *
 * @author Stuart Clark
 */
public abstract class IntensityHist implements Feature {

  private static final int HIST_SIZE = 256;

  /**
   * The number of numBins that should be used for the histogram.
   */
  private int numBins;

  public IntensityHist(int numBins) {
    this.numBins = numBins;
  }

  protected Histogram createHist(ROI roi, Mat mat) throws LungsException {
    if (mat.channels() != 1) {
      throw new LungsException("mat must have 1 channel");
    }

    List<Point> points = roi.getRegion();

    // Count up the number of occurrences for each value
    double[] valCount = new double[HIST_SIZE];
    for (Point point : roi.getRegion()) {
      int val = (int) mat.get((int) point.y, (int) point.x)[0];
      valCount[val]++;
    }

    // Create a histogram with the correct number of bins
    double[] hist = new double[numBins];
    int binIndex = 0;
    int valPerBin = (int) Math.ceil(HIST_SIZE / numBins);
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

    return new Histogram(hist);
  }

}
