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
    MinMaxXY<Double> mmXY = xyMaxMin(perimeter);

    // Create a mat with the filled region. +1's as there will be a 0th row and col
    Mat filled = Mat.zeros(mmXY.maxY.intValue() + 1, mmXY.maxX.intValue() + 1, CvType.CV_8UC1);
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

  /**
   * @param regionPoints as single region given as a list of all of it's {@link Point}s.
   * @return a list of all the points that form the inclusive parameter of the region. i.e. the
   *         points are also part of the area. List is not guaranteed to be in raster order.
   */
  public static List<Point> region2perim(List<Point> regionPoints) throws LungsException {
    MinMaxXY<Double> mmXY = xyMaxMin(regionPoints);
    Mat region = points2MinMat(regionPoints, mmXY);
    List<MatOfPoint> contours = new ArrayList<>();

    // Find external contour
    Imgproc.findContours(region, contours, new Mat(), Imgproc.RETR_EXTERNAL,
        Imgproc.CHAIN_APPROX_NONE);

    // There should only every be one contour
    List<Point> perimeter = contours.get(0).toList();
    // add the min vals back to each x and y
    perimeter.forEach(p -> {
      p.x += mmXY.minX;
      p.y += mmXY.minY;
    });

    return perimeter;
  }

  /**
   * @param points
   * @return a {@link MinMaxXY<Double>} holding the minimum and maximum values for the x and y
   *         co-ordinates given in {@code points}.
   */
  public static MinMaxXY<Double> xyMaxMin(List<Point> points) {
    // Get first point
    Point first = points.get(0);

    // Find max x and y values and correctly set the first point
    MinMaxXY<Double> mmXY = new MinMaxXY<>();
    mmXY.minX = first.x;
    mmXY.maxX = first.x;
    mmXY.minY = first.y;
    mmXY.maxY = first.y;
    for (int i = 1; i < points.size(); i++) {
      Point point = points.get(i);

      if (point.x > mmXY.maxX) {
        mmXY.maxX = point.x;
      } else if (point.x < mmXY.minX) {
        mmXY.minX = point.x;
      }

      if (point.y > mmXY.maxY) {
        mmXY.maxY = point.y;
      } else if (point.y < mmXY.minY) {
        mmXY.minY = point.y;
      }

    }

    return mmXY;

  }

  /**
   * @param points
   * @return a binary {Mat} with pixels values of {code FORE_GROUND} at each of the {@code points}.
   *         The point are offset so that the size of the Mat is reduced to the minimum size
   *         required.
   */
  public static Mat points2MinMat(List<Point> points) {
    MinMaxXY<Double> mmXY = xyMaxMin(points);
    return points2MinMat(points, mmXY);
  }

  /**
   * @param points
   * @param mmXY the {@link MinMaxXY<Double>} obtained using {@link PointUtils#xyMaxMin(List)}.
   * @return a binary {Mat} with pixels values of {code FORE_GROUND} at each of the {@code points}.
   *         The point are offset so that the size of the Mat is reduced to the minimum size
   *         required.
   */
  public static Mat points2MinMat(List<Point> points, MinMaxXY<Double> mmXY) {
    // Get mins maxes and ranges
    double rangeX = mmXY.maxX - mmXY.minX;
    double rangeY = mmXY.maxY - mmXY.minY;

    // Crete mat with regionPoints
    Mat ma = Mat.zeros((int) rangeY + 1, (int) rangeX + 1, CvType.CV_8UC1);
    for (Point point : points) {
      int row = (int) (point.y - mmXY.minY);
      int col = (int) (point.x - mmXY.minX);
      ma.put(row, col, FORE_GROUND);
    }

    return ma;
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
