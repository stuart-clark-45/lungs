package ml.feature;

import org.opencv.core.Mat;

import model.ROI;
import util.MatUtils;

/**
 * Computes the mean intensity of the {@link ROI} and updates {@link ROI#meanIntensity}.
 *
 * @author Stuart Clark
 */
public class MeanIntensity implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) {
    roi.setMeanIntensity(MatUtils.mean(mat, roi.getRegion()));
  }

}
