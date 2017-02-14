package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Computes the perimeter length for the {@link ROI} and updates {@link ROI#perimLength}.
 *
 * @author Stuart Clark
 */
public class Perimeter implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    // TODO need to measure using info from meta data not just count number of pixels
    roi.setPerimLength(roi.getContour().size());
  }

}
