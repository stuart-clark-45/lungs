package ml.feature;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import core.Lungs;
import model.roi.Circle;
import model.roi.ROI;
import util.ColourBGR;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class MinCircleTest {

  @Test
  public void test() throws Exception {
    // Create the roi
    List<Point> contour = new ArrayList<>();
    contour.add(new Point(4, 5));
    contour.add(new Point(4, 6));
    contour.add(new Point(4, 7));
    contour.add(new Point(4, 8));
    contour.add(new Point(4, 9));
    ROI roi = new ROI();
    roi.setContour(contour);

    // Compute the min circle
    MinCircle minCircle = new MinCircle();
    minCircle.compute(roi, null);

    // Check computed values
    Circle circle = roi.getMinCircle();
    double radius = circle.getRadius();
    Point center = circle.getCenter();
    assertEquals(2d, radius, 0.0001);
    assertEquals(new Point(4, 7), center);

    // Draw the roi and the min circle
    Mat rgb = Mat.zeros(20, 20, CvType.CV_8UC3);
    roi.setRegion(contour);
    new Lungs().paintROI(rgb, roi, ColourBGR.RED);
    Imgproc.circle(rgb, center, (int) radius, new Scalar(ColourBGR.BLUE));

    // Uncomment to view roi and minimum bounding circle
    // new MatViewer(rgb).display();
  }

}
