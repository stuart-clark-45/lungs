package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;
import util.LungsException;

/**
 * Should be implemented by all features.
 *
 * @author Stuart Clark
 */
public interface Feature {

  /**
   * Compute the feature and set the result as a field on {@code roi}.
   * 
   * @param roi
   */
  void compute(ROI roi, Mat mat) throws LungsException;

}
