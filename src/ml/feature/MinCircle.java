package ml.feature;

import model.roi.Circle;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import model.roi.ROI;
import util.LungsException;

/**
 * Computes the minimum fitting circle for the {@link ROI} and updates {@link ROI#minCircle}.
 *
 * @author Stuart Clark
 */
public class MinCircle implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    // Calculate the min circle
    Point center = new Point();
    MatOfPoint2f matOfPoints = new MatOfPoint2f();
    matOfPoints.fromList(roi.getContour());
    float[] radius = new float[1];
    Imgproc.minEnclosingCircle(matOfPoints, center, radius);

    // Store the result in the roi
    Circle minCircle = new Circle();
    minCircle.setCenter(center);
    minCircle.setRadius(radius[0]);
    roi.setMinCircle(minCircle);
  }

}
