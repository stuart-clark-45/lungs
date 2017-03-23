package ml.feature;

import static java.lang.Math.pow;
import static util.MatUtils.get;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.Histogram;
import model.ROI;
import util.LungsException;

/**
 * Creates a histogram for a Local Quaternary Pattern and assigns it to the {@link ROI}.
 *
 * @author Stuart Clark
 */
public class LQP implements Feature {

  static final int NUM_BINS = 64;
  static final int NUM_POS_VAL = 65536;

  /**
   * See {@link LQP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int LT = 0;

  /**
   * See {@link LQP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int EQ = 1;

  /**
   * See {@link LQP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int GT = 2;

  /**
   * See {@link LQP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int VOID = 3;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    List<Point> region = roi.getRegion();
    Set<Point> regionSet = new HashSet<>(region);

    Histogram histogram = new Histogram(NUM_BINS, NUM_POS_VAL);

    for (Point point : region) {
      int value = 0;

      // Up Left
      value +=
          pow(4, 0) * neighbourValue(point, new Point(point.x - 1, point.y - 1), regionSet, mat);

      // Up
      value += pow(4, 1) * neighbourValue(point, new Point(point.x, point.y - 1), regionSet, mat);

      // Up Right
      value +=
          pow(4, 2) * neighbourValue(point, new Point(point.x + 1, point.y - 1), regionSet, mat);

      // Right
      value += pow(4, 3) * neighbourValue(point, new Point(point.x - 1, point.y), regionSet, mat);

      // Down Right
      value +=
          pow(4, 4) * neighbourValue(point, new Point(point.x + 1, point.y + 1), regionSet, mat);

      // Down
      value += pow(4, 5) * neighbourValue(point, new Point(point.x, point.y + 1), regionSet, mat);

      // Down Left
      value +=
          pow(4, 6) * neighbourValue(point, new Point(point.x - 1, point.y + 1), regionSet, mat);

      // Left
      value += pow(4, 7) * neighbourValue(point, new Point(point.x - 1, point.y), regionSet, mat);

      // Add the value to the histogram
      histogram.add(value);
    }

    // Compute the histogram bins
    histogram.computeBins();
    histogram.toFrequencies();

    // Store in the ROI
    roi.setLqp(histogram);
  }

  /**
   * Compute the pattern value for the neighbouring pixel.
   *
   * @param point
   * @param neighbour a neighbour of {@code point}.
   * @param regionSet the set of all the points in the region.
   * @param mat the {@link Mat} the {@code point} belongs too.
   * @return <ul>
   *         <li>{@code LT} if the value for the pixel at {@code neighbour} is less than the value
   *         for the pixel at {@code point}</li>
   *         <li>{@code GT} if the value for the pixel at {@code neighbour} is greater than the
   *         value for the pixel at {@code point}</li>
   *         <li>{@code EQ} if the value for the pixel at {@code neighbour} is equal to the value
   *         for the pixel at {@code point}</li>
   *         <li>{@code VOID} if {@code neighbour} is not part of the region.
   *         <ul/>
   */
  private int neighbourValue(Point point, Point neighbour, Set<Point> regionSet, Mat mat) {
    if (regionSet.contains(neighbour)) {
      double pointVal = get(mat, point)[0];
      double neighbourVal = get(mat, neighbour)[0];

      if (neighbourVal < pointVal) {
        return LT;
      } else if (neighbourVal > pointVal) {
        return GT;
      } else {
        return EQ;
      }

    } else {
      return VOID;
    }
  }
}
