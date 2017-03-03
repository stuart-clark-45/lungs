package ml.feature;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

import model.ROI;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class BoundingBoxTest {

  @Test
  public void test() throws Exception {
    // Create the roi
    List<Point> contour = new ArrayList<>();
    contour.add(new Point(2, 6));
    contour.add(new Point(3, 5));
    contour.add(new Point(1, 5));
    contour.add(new Point(4, 4));
    contour.add(new Point(0, 4));
    contour.add(new Point(3, 3));
    contour.add(new Point(1, 3));
    contour.add(new Point(2, 2));

    // Create the ROI and call BoundingBox.compute(..)
    ROI roi = new ROI();
    roi.setContour(contour);
    new BoundingBox().compute(roi, null);
    RotatedRect boundingBox = roi.getBoundingBox();

    // Check the center point is correct
    assertEquals(new Point(2, 4), boundingBox.center);

    // Get the points for the corners of the bounding box
    Point points[] = new Point[4];
    boundingBox.points(points);
    // Round them to the nearest integer
    Set<Point> roundedPoints = new HashSet<>();
    for (Point point : points) {
      roundedPoints.add(new Point(Math.round(point.x), Math.round(point.y)));
    }

    // Check the corner points are as expected
    Set<Point> expectedPoints = new HashSet<>();
    expectedPoints.add(new Point(2,6));
    expectedPoints.add(new Point(0,4));
    expectedPoints.add(new Point(4,4));
    expectedPoints.add(new Point(2,2));
    assertEquals(expectedPoints, roundedPoints);

    // Check the elongation value if correct (should be zero as width and height are the same)
    assertEquals(Double.valueOf(0), roi.getElongation());
  }
}
