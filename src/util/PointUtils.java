package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.opencv.core.Point;

/**
 * Suite of utility methods associated with {@link Point}s
 *
 * @author Stuart Clark
 */
public class PointUtils {

  private PointUtils() {
    // Hide the constructor
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
