package vision;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opencv.core.Point;

import model.GroundTruth;
import model.ROI;

/**
 * @author Stuart Clark
 */
public class MatcherTest {

  @Test
  public void test() throws Exception {
    List<Point> points1 = new ArrayList<>();
    points1.add(new Point(1, 1));
    points1.add(new Point(2, 1));
    points1.add(new Point(1, 2));
    points1.add(new Point(2, 2));

    ROI roi = new ROI();
    roi.setPoints(points1);
    GroundTruth gt = new GroundTruth();
    gt.setRegion(points1);
    assertEquals(Double.valueOf(1), Double.valueOf(Matcher.match(roi, gt)));

    List<Point> points2 = new ArrayList<>();
    points2.add(new Point(2, 1));
    points2.add(new Point(3, 1));
    points2.add(new Point(2, 2));
    points2.add(new Point(3, 2));
    assertEquals(Double.valueOf(2.0 / 6.0), Double.valueOf(Matcher.match(points1, points2)));
  }

}
