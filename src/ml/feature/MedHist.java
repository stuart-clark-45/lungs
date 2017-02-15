package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram with a medium amount of bins for the {@link ROI} and updates {@link ROI#medHist}.
 *
 * @author Stuart Clark
 */
public class MedHist extends IntensityHist {

  public MedHist() {
    super(100);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setMedHist(createHist(roi, mat));
  }

}
