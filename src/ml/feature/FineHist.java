package ml.feature;

import static model.Histogram.POS_VALS_8BIT;

import org.opencv.core.Mat;

import model.Histogram;
import model.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with lots of bins and updates {@link ROI#fineHist}.
 *
 * @author Stuart Clark
 */
public class FineHist implements Feature {

  public static final int BINS = 128;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    Histogram histogram = new Histogram(BINS, POS_VALS_8BIT);
    histogram.add(roi.getRegion(), mat);
    histogram.computeBins();
    histogram.toFrequencies();
    roi.setFineHist(histogram);
  }

}
