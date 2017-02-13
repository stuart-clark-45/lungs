package ml.feature;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.ROI;
import util.LungsException;

/**
 * Computes the intensity histogram for {@link ROI} and updates {@link ROI#intensityHistogram}.
 *
 * @author Stuart Clark
 */
public class IntensityHistogram implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    if (mat.channels() != 1) {
      throw new LungsException("mat must have 1 channel");
    }

    double[] hist = new double[mat.depth() + 1];
    List<Point> points = roi.getPoints();

    // Count up the number of occurrences for each value
    for (Point point : roi.getPoints()) {
      int val = (int) mat.get((int) point.y, (int) point.x)[0];
      hist[val]++;
    }

    // Covert to frequencies
    for (int i = 0; i < hist.length; i++) {
      hist[i] /= points.size();
    }

    // Update roi
    roi.setIntensityHistogram(hist);
  }

}
