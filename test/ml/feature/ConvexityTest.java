package ml.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Point;

import model.ROI;
import util.PointUtils;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class ConvexityTest {

  @Test
  public void test() throws Exception {
    // Create a square
    List<Point> squareRegion = new ArrayList<>();
    squareRegion.add(new Point(4, 5));
    squareRegion.add(new Point(4, 6));
    squareRegion.add(new Point(4, 7));
    squareRegion.add(new Point(4, 8));
    squareRegion.add(new Point(4, 9));

    squareRegion.add(new Point(5, 5));
    squareRegion.add(new Point(5, 6));
    squareRegion.add(new Point(5, 7));
    squareRegion.add(new Point(5, 8));
    squareRegion.add(new Point(5, 9));

    squareRegion.add(new Point(6, 5));
    squareRegion.add(new Point(6, 6));
    squareRegion.add(new Point(6, 7));
    squareRegion.add(new Point(6, 8));
    squareRegion.add(new Point(6, 9));

    squareRegion.add(new Point(7, 5));
    squareRegion.add(new Point(7, 6));
    squareRegion.add(new Point(7, 7));
    squareRegion.add(new Point(7, 8));
    squareRegion.add(new Point(7, 9));

    squareRegion.add(new Point(8, 5));
    squareRegion.add(new Point(8, 6));
    squareRegion.add(new Point(8, 7));
    squareRegion.add(new Point(8, 8));
    squareRegion.add(new Point(8, 9));

    ROI square = new ROI();
    square.setRegion(PointUtils.perim2Region(squareRegion, true));

    // Create a square
    List<Point> crossRegion = new ArrayList<>();
    crossRegion.add(new Point(4, 7));

    crossRegion.add(new Point(5, 7));

    crossRegion.add(new Point(6, 5));
    crossRegion.add(new Point(6, 6));
    crossRegion.add(new Point(6, 7));
    crossRegion.add(new Point(6, 8));
    crossRegion.add(new Point(6, 9));

    crossRegion.add(new Point(7, 7));

    crossRegion.add(new Point(8, 7));

    ROI cross = new ROI();
    cross.setRegion(PointUtils.perim2Region(crossRegion, true));

    Convexity convexity = new Convexity();
    convexity.compute(square, null);
    convexity.compute(cross, null);

    assertEquals(Double.valueOf(1), square.getConvexity());
    assertTrue(cross.getConvexity() < 1);

    // Uncomment to view the regions created
    // new MatViewer(points2MinMat(squareRegion, xyMaxMin(squareRegion), null), points2MinMat(
    // crossRegion, xyMaxMin(crossRegion), null)).display();
  }
}
