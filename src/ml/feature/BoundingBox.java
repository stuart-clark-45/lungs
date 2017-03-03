package ml.feature;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import model.ROI;
import util.LungsException;

/**
 * Fits a minimum bounding box to the {@link ROI} and updates {@link ROI#boundingBox}. Also computed
 * the rotation invariant elongation of the bounding box and updates {@link ROI#elongation}.
 *
 * @author Stuart Clark
 */
public class BoundingBox implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    // Compute the bounding box
    MatOfPoint2f matOfPoint = new MatOfPoint2f();
    matOfPoint.fromList(roi.getContour());
    RotatedRect boundingBox = Imgproc.minAreaRect(matOfPoint);
    roi.setBoundingBox(boundingBox);

    // Compute rotation invariant elongation
    int width = boundingBox.boundingRect().width;
    int height = boundingBox.boundingRect().height;
    roi.setElongation(1 - (Math.min(width, height) / (double) Math.max(width, height)));
  }

}
