package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Creates a histogram with lots of bins for the {@link ROI} and updates {@link ROI#fineHist}.
 *
 * @author Stuart Clark
 */
public class FineHist extends IntensityHist {

  public FineHist() {
    super(250);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setFineHist(createHist(roi, mat));
  }

}
