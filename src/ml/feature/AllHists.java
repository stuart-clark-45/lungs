package ml.feature;

import static model.Histogram.POS_VALS_8BIT;
import static model.Histogram.sturges;

import org.opencv.core.Mat;

import model.Histogram;
import model.ROI;
import model.ROIAreaStats;
import util.LungsException;

/**
 * Creates histograms for the {@link ROI} with different numbers of bins and updates the {@link ROI}
 * .
 *
 * @author Stuart Clark
 */
public class AllHists implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    // Create the fine histogram and add it to the roi
    Histogram fine = new Histogram(getFine(), POS_VALS_8BIT);
    fine.add(roi.getRegion(), mat);
    fine.computeBins();
    fine.toFrequencies();
    roi.setFineHist(fine);

    // Create the coarse histogram and add it to the roi
    Histogram coarse = new Histogram(getCoarse(), fine);
    coarse.computeBins();
    coarse.toFrequencies();
    roi.setCoarseHist(coarse);
  }

  /**
   * @return the number of bins used in a coarse histogram i.e. {@link ROI#coarseHist}.
   */
  public static int getCoarse() {
    ROIAreaStats stats = ROIAreaStats.get();
    return sturges(POS_VALS_8BIT, stats.getMean());
  }

  /**
   * @return the number of bins used in a fine histogram i.e. {@link ROI#fineHist}.
   */
  public static int getFine() {
    ROIAreaStats stats = ROIAreaStats.get();
    return sturges(POS_VALS_8BIT, stats.getMax());
  }

}
