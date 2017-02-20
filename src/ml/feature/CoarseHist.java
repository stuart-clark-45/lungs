package ml.feature;

import model.roi.Histogram;
import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with few bins and updates {@link ROI#coarseHist}.
 *
 * @author Stuart Clark
 */
public class CoarseHist implements Feature {

  public static final int BINS = 16;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setCoarseHist(Histogram.createHist(roi.getRegion(), mat, BINS));
  }

}
