package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram for the {@link ROI} with a medium amount of bins and updates
 * {@link ROI#medHist}.
 *
 * @author Stuart Clark
 */
public class MedHist extends IntensityHist {

  public static final int BINS = 128;

  public MedHist() {
    super(BINS);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    createHist(roi, mat);
    roi.setMedHist(getHistogram());
  }

}
