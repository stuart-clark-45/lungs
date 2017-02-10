package ml;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

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
    double mean = 0;
    List<Point> points = roi.getPoints();
    for (Point point : points) {
      mean += MatUtils.getIntensity(mat, point);
    }
    mean = mean / points.size();
    roi.setMeanIntensity(mean);
  }

}
