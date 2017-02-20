package ml.feature;

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

  public static final int BINS = 128;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setMedHist(Histogram.createHist(roi.getRegion(), mat, BINS));
  }

}
