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
 * Creates a histogram for a Local Ternary Pattern and assigns it to the {@link ROI}.
 *
 * @author Stuart Clark
 */
public class LTP implements Feature {

  public static final int BINS = 64;
  static final int BASE = 3;
  static final int NUM_POS_VAL = (int) Math.pow(BASE, 8);

  /**
   * See {@link LTP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int LT = 0;

  /**
   * See {@link LTP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int GTE = 1;

  /**
   * See {@link LTP#neighbourValue(Point, Point, Set, Mat)}.
   */
  static final int VOID = 2;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    List<Point> region = roi.getRegion();
    Set<Point> regionSet = new HashSet<>(region);

    Histogram histogram = new Histogram(BINS, NUM_POS_VAL);

    for (Point point : region) {
      int value = 0;

      // Up Left
      value +=
          pow(BASE, 0) * neighbourValue(point, new Point(point.x - 1, point.y - 1), regionSet, mat);

      // Up
      value +=
          pow(BASE, 1) * neighbourValue(point, new Point(point.x, point.y - 1), regionSet, mat);

      // Up Right
      value +=
          pow(BASE, 2) * neighbourValue(point, new Point(point.x + 1, point.y - 1), regionSet, mat);

      // Right
      value +=
          pow(BASE, 3) * neighbourValue(point, new Point(point.x - 1, point.y), regionSet, mat);

      // Down Right
      value +=
          pow(BASE, 4)
              * neighbourValue(point, new Point(point.x + 1, point.y + 1), regionSet, mat);

      // Down
      value +=
          pow(BASE, 5) * neighbourValue(point, new Point(point.x, point.y + 1), regionSet, mat);

      // Down Left
      value +=
          pow(BASE, 6) * neighbourValue(point, new Point(point.x - 1, point.y + 1), regionSet, mat);

      // Left
      value +=
          pow(BASE, 7) * neighbourValue(point, new Point(point.x - 1, point.y), regionSet, mat);

      // Add the value to the histogram
      histogram.add(value);
    }

    // Compute the histogram bins
    histogram.computeBins();
    histogram.toFrequencies();

    // Store in the ROI
    roi.setLtp(histogram);
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
   *         <li>{@code GTE} if the value for the pixel at {@code neighbour} is greater than or equal to the
   *         value for the pixel at {@code point}</li>
   *         <li>{@code VOID} if {@code neighbour} is not part of the region.
   *         <ul/>
   */
  private int neighbourValue(Point point, Point neighbour, Set<Point> regionSet, Mat mat) {
    if (regionSet.contains(neighbour)) {
      double pointVal = get(mat, point)[0];
      double neighbourVal = get(mat, neighbour)[0];
      return neighbourVal < pointVal ? LT : GTE;

    } else {
      return VOID;
    }
  }
}
