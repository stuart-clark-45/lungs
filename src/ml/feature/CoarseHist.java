package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with few bins and updates {@link ROI#coarseHist}.
 *
 * @author Stuart Clark
 */
public class CoarseHist extends IntensityHist {

  public static final int BINS = 16;

  public CoarseHist() {
    super(BINS);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setCoarseHist(createHist(roi, mat));
  }

}
