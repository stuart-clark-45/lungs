package ml.feature;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import model.ROI;
import util.LungsException;

/**
 * Computes the radius of the minimum fitting circle for the {@link ROI} and updates
 * {@link ROI#radius}.
 *
 * @author Stuart Clark
 */
public class Radius implements Feature {

  /**
   * Only used by unit test.
   */
  Point lastCenter;

  public Radius() {
    lastCenter = new Point();
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    MatOfPoint2f matOfPoints = new MatOfPoint2f();
    matOfPoints.fromList(roi.getContour());
    float[] radius = new float[1];
    Imgproc.minEnclosingCircle(matOfPoints, lastCenter, radius);
    roi.setRadius(radius[0]);
  }

}
