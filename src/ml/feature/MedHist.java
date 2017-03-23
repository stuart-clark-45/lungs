package ml.feature;

import static model.Histogram.POS_VALS_8BIT;

import org.opencv.core.Mat;

import model.Histogram;
import model.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with a medium amount of bins and updates
 * {@link ROI#medHist}.
 *
 * @author Stuart Clark
 */
public class MedHist implements Feature {

  public static final int BINS = 64;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    Histogram histogram = new Histogram(POS_VALS_8BIT);
    histogram.createHist(roi.getRegion(), mat, BINS);
    roi.setMedHist(histogram);
  }

}
