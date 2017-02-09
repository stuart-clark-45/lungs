package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import model.MinMaxXY;
import vision.ROIExtractor;

/**
 * Suite of utility methods associated with {@link Point}s.
 *
 * @author Stuart Clark
 */
public class PointUtils {

  private static final int FORE_GROUND = 255;

  private PointUtils() {
    // Hide the constructor
  }

  /**
   * @param perimeter a list of {@link Point}s, must be an unbroken parameter.
   * @param inclusive true if the points of the parameter should be included in the returned region,
   *        false otherwise.
   * @return a list of {@link Point}s that contains every {@link Point} in the region that
   *         corresponds to {@code perimeter}. Order is not guaranteed to be maintained.
   */
  public static List<Point> perim2Region(List<Point> perimeter, boolean inclusive)
      throws LungsException {
    // Remove duplicates if there are any
    perimeter = dedupe(perimeter);

    // Calculate the min and max and and y values
    MinMaxXY<Double> xyMM = xyMaxMin(perimeter);

    // Create a mat with the filled region
    Mat filled =
        Mat.zeros(xyMM.getMaxY().intValue() + 1, xyMM.getMaxX().intValue() + 1, CvType.CV_8UC1);
    Point[] pointArray = perimeter.toArray(new Point[perimeter.size()]);
    Imgproc.fillPoly(filled, Collections.singletonList(new MatOfPoint(pointArray)), new Scalar(
        FORE_GROUND));

    // Extract the region points
    ROIExtractor extractor = new ROIExtractor(FORE_GROUND);
    Point point = perimeter.get(0);
    List<Point> region = extractor.extractOne(filled, point).getPoints();

    // Remove the perimeter if it was not inclusive
    if (!inclusive) {
      region.removeAll(perimeter);
    }

    return region;
  }

  public static MinMaxXY<Double> xyMaxMin(List<Point> points) {
    // Get first point
    Point first = points.get(0);

    // Find max x and y values and correctly set the first point
    MinMaxXY<Double> mmXY = new MinMaxXY<>();
    mmXY.setMinX(first.x);
    mmXY.setMaxX(first.x);
    mmXY.setMinY(first.y);
    mmXY.setMinY(first.y);
    for (int i = 1; i < points.size(); i++) {
      Point point = points.get(i);

      if (point.x > mmXY.getMaxX()) {
        mmXY.setMaxX(point.x);
      } else if (point.x < mmXY.getMinX()) {
        mmXY.setMinX(point.x);
      }

      if (point.y > mmXY.getMaxY()) {
        mmXY.setMaxY(point.y);
      } else if (point.y < mmXY.getMinY()) {
        mmXY.setMinY(point.y);
      }

    }

    return mmXY;

  }

  /**
   * @param points
   * @return {@code points} with any duplicates removed and order maintained.
   */
  public static List<Point> dedupe(List<Point> points) {
    List<Point> noDupes = new ArrayList<>(points.size());
    Set<Point> seen = new HashSet<>();

    for (Point next : points) {
      if (!seen.contains(next)) {
        noDupes.add(next);
        seen.add(next);
      }
    }

    return noDupes;
  }

  /**
   * @param unsorted
   * @return a list of {@link Point}s sorted into the order in which they would be encountered
   *         during a raster scan.
   */
  public static List<Point> xySort(List<Point> unsorted) {
    // Group the points by y co-ordinate
    MultiMap<Double, Point> yGrouped = new MultiMap<>();
    unsorted.forEach(p -> yGrouped.putOne(p.y, p));

    // Sort each of the lists in the multi map so the x values are in ascending order
    yGrouped.values().forEach(points -> points.sort(Comparator.comparingDouble(p -> p.x)));

    List<Point> sorted = new ArrayList<>(unsorted.size());

    // Sort the entries by y co-ordinate
    yGrouped.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getKey))

    // Add each of the points to sorted
        .forEach(e -> sorted.addAll(e.getValue()));

    return sorted;
  }

}
