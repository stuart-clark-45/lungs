package ml.feature;

import org.opencv.core.Mat;

import model.roi.Histogram;
import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with lots of bins and updates {@link ROI#fineHist}.
 *
 * @author Stuart Clark
 */
public class FineHist implements Feature {

  public static final int BINS = 256;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setFineHist(Histogram.createHist(roi.getRegion(), mat, BINS));
  }

}
