package ml.feature;

import org.opencv.core.Mat;

import model.roi.ROI;

/**
 * Computes the area of the {@link ROI} and updates {@link ROI#area}.
 * 
 * @author Stuart Clark
 */
public class Area implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) {
    // TODO need to measure using info from meta data not just count number of pixels
    roi.setArea(roi.getRegion().size());
  }

}
