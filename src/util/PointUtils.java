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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.sun.istack.internal.Nullable;

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
    Point point = perimeter.get(0);
    List<Point> region = ROIExtractor.extractOne(filled, point).getRegion();

    // Remove the perimeter if it was not inclusive
    if (!inclusive) {
      region.removeAll(perimeter);
    }

    return region;
  }

  /**
   * @param regionPoints as single region given as a list of all of it's {@link Point}s.
   * @return a list of all the points that form the inclusive contour of the region. i.e. the points
   *         of the countor are also part of the region. List is not guaranteed to be in raster
   *         order.
   */
  public static List<Point> region2Contour(List<Point> regionPoints) {
    MinMaxXY<Double> mmXY = xyMaxMin(regionPoints);
    Mat region = points2MinMat(regionPoints, mmXY, null);
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
   * @param mmXY the {@link MinMaxXY<Double>} obtained using {@link PointUtils#xyMaxMin(List)}.
   * @param original the {@link Mat} to take the pixel values from for the min {@link Mat} being
   *        created. If null then {@code FORE_GROUND} will be used for all points.
   * @return a binary {Mat} with pixels values of {code FORE_GROUND} at each of the {@code points}.
   *         The point are offset so that the size of the Mat is reduced to the minimum size
   *         required.
   */
  public static Mat points2MinMat(List<Point> points, MinMaxXY<Double> mmXY, @Nullable Mat original) {
    // Get mins maxes and ranges
    double rangeX = mmXY.maxX - mmXY.minX;
    double rangeY = mmXY.maxY - mmXY.minY;

    // Crete mat with regionPoints
    Mat mat = Mat.zeros((int) rangeY + 1, (int) rangeX + 1, CvType.CV_8UC1);
    for (Point point : points) {
      int row = (int) (point.y - mmXY.minY);
      int col = (int) (point.x - mmXY.minX);
      if (original != null) {
        mat.put(row, col, MatUtils.get(original, point));
      } else {
        mat.put(row, col, FORE_GROUND);
      }
    }

    return mat;
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

  /**
   * @param points
   * @return the centroid of the list of points given.
   */
  public static Point centroid(List<Point> points) {
    double x = 0;
    double y = 0;
    for (Point point : points) {
      x += point.x;
      y += point.y;
    }
    int nPoints = points.size();
    x = x / nPoints;
    y = y / nPoints;
    return new Point(x, y);
  }

  /**
   * @param contour the contour of the ground truth
   * @return the radius of the smallest circle that can be fitted to {@code contour}.
   */
  public static double minCircleRadius(List<Point> contour) {
    Point center = new Point();
    MatOfPoint2f matOfPoints = new MatOfPoint2f();
    matOfPoints.fromList(contour);
    float[] radius = new float[1];
    Imgproc.minEnclosingCircle(matOfPoints, center, radius);
    return radius[0];
  }

}
