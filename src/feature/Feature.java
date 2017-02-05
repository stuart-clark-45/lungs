package feature;

import org.opencv.core.Mat;

import model.ROI;

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
  void compute(ROI roi, Mat mat);

}
