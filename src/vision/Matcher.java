package vision;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opencv.core.Point;

import model.GroundTruth;
import model.ROI;

/**
 * Used to perform a "fuzzy" match between regions.
 *
 * @author Stuart Clark
 */
public class Matcher {

  /**
   * Calculate the degree to which a two {@link GroundTruth}s match.
   *
   * @param gt1
   * @param gt2
   * @return a value between 0 and 1 inclusive. 1 is a perfect match, 0 is no match at all.
   */
  public static double match(GroundTruth gt1, GroundTruth gt2) {
    return match(gt1.getRegion(), gt2.getRegion());
  }

  /**
   * Calculate the degree to which a {@link ROI} and {@link GroundTruth} match.
   *
   * @param roi
   * @param gt
   * @return a value between 0 and 1 inclusive. 1 is a perfect match, 0 is no match at all.
   */
  public static double match(ROI roi, GroundTruth gt) {
    return match(roi.getPoints(), gt.getRegion());
  }

  /**
   * Calculate the degree to which a two regions overlap.
   *
   * @param points1 a set of points that define a region.
   * @param points2 a set of points that define a region.
   * @return a value between 0 and 1 inclusive. 1 is a perfect match, 0 is no match at all.
   */
  public static double match(Collection<Point> points1, Collection<Point> points2) {
    Set<Point> intersection = new HashSet<>(points1);
    intersection.retainAll(points2);

    Set<Point> unison = new HashSet<>(points1);
    unison.addAll(points2);

    return intersection.size() / (double) unison.size();
  }

}
