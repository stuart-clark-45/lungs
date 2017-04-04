package ml.feature;

import org.opencv.core.Mat;

import model.ROI;
import util.LungsException;

/**
 * Computes the perimeter length in pixels for the {@link ROI} and updates {@link ROI#perimLength}.
 *
 * @author Stuart Clark
 */
public class Perimeter implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    roi.setPerimLength(roi.getContour().size());
  }

}
