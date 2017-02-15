package ml.feature;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import model.roi.ROI;
import util.LungsException;

/**
 * Fits an ellipse to the {@link ROI} and updates {@link ROI#fitEllipse}.
 *
 * @author Stuart Clark
 */
public class FitEllipse implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    MatOfPoint2f matOfPoint = new MatOfPoint2f();
    matOfPoint.fromList(roi.getContour());
    roi.setFitEllipse(Imgproc.fitEllipse(matOfPoint));
  }

}
