package ml.feature;

import org.opencv.core.Mat;

import model.ROI;
import util.LungsException;

/**
 * Used to compute the value for {@link ROI#circularity}. {@link Area#compute(ROI, Mat)} and
 * {@link MinCircle#compute(ROI, Mat)} must have both been called on {@code roi} before
 * {@link Circularity#compute(ROI, Mat)}.
 *
 * @author Stuart Clark
 */
public class Circularity implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    double minCircleArea = Math.PI * Math.pow(roi.getMinCircle().getRadius(), 2);
    Integer area = roi.getArea();
    roi.setCircularity(Math.min(area, minCircleArea) / Math.max(area, minCircleArea));
  }

}
