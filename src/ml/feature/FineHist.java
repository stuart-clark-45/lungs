package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with lots of bins and updates {@link ROI#fineHist}.
 *
 * @author Stuart Clark
 */
public class FineHist extends IntensityHist {

  public static final int BINS = 256;

  public FineHist() {
    super(BINS);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    createHist(roi, mat);
    roi.setFineHist(getHistogram());
  }

}
