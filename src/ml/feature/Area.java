package ml.feature;

import org.opencv.core.Mat;

import model.ROI;

/**
 * Computes the area of the {@link ROI} in pixels and updates {@link ROI#area}.
 * 
 * @author Stuart Clark
 */
public class Area implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) {
    roi.setArea(roi.getRegion().size());
  }

}
