package ml.feature;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import model.ROI;
import util.LungsException;

import java.util.List;

/**
 * Fits an ellipse to the {@link ROI} and updates {@link ROI#fitEllipse}.
 *
 * @author Stuart Clark
 */
public class FitEllipse implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    List<Point> contour = roi.getContour();

    // Check if contour is large enough
    if(contour.size() < 5){
      return;
    }

    MatOfPoint2f matOfPoint = new MatOfPoint2f();
    matOfPoint.fromList(contour);
    roi.setFitEllipse(Imgproc.fitEllipse(matOfPoint));
  }

}
