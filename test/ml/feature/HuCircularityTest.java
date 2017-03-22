package ml.feature;

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
public class HuCircularityTest {

  @Test
  public void test() throws Exception {
    // Create a square
    List<Point> squareContour = new ArrayList<>();
    squareContour.add(new Point(4, 5));
    squareContour.add(new Point(5, 5));
    squareContour.add(new Point(6, 5));
    squareContour.add(new Point(6, 6));
    squareContour.add(new Point(6, 7));
    squareContour.add(new Point(5, 7));
    squareContour.add(new Point(4, 7));
    squareContour.add(new Point(4, 6));
    ROI square = new ROI();
    square.setContour(squareContour);
    square.setRegion(PointUtils.perim2Region(squareContour, true));

    // Create a line
    List<Point> lineContour = new ArrayList<>();
    lineContour.add(new Point(4, 5));
    lineContour.add(new Point(4, 6));
    lineContour.add(new Point(4, 7));
    lineContour.add(new Point(4, 8));
    lineContour.add(new Point(4, 9));
    ROI line = new ROI();
    line.setContour(lineContour);
    line.setRegion(PointUtils.perim2Region(lineContour, true));

    HuCircularity circularity = new HuCircularity();
    circularity.compute(square, null);
    circularity.compute(line, null);

    double squareCircularity = square.getHuCircularity();
    double lineCircularity = line.getHuCircularity();

    // Check between 0 and 1
    assertTrue(squareCircularity > 0 && squareCircularity < 1);
    assertTrue(lineCircularity > 0 && lineCircularity < 1);

    // The square should be more like a circle than the line
    assertTrue(squareCircularity > lineCircularity);
  }

}
