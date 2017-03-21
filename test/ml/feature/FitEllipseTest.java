package ml.feature;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import core.Lungs;
import model.ROI;
import util.ColourBGR;
import util.Testing;


/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class FitEllipseTest {

  @Test
  public void test() throws Exception {
    // Create the roi
    List<Point> contour = new ArrayList<>();
    contour.add(new Point(4, 5));
    contour.add(new Point(4, 6));
    contour.add(new Point(3, 7));
    contour.add(new Point(4, 7));
    contour.add(new Point(5, 7));
    contour.add(new Point(4, 8));
    contour.add(new Point(4, 9));
    ROI roi = new ROI();
    roi.setContour(contour);

    new FitEllipse().compute(roi, null);

    // Check correct ellipse
    RotatedRect fitEllipse = roi.getFitEllipse();
    assertEquals(Double.valueOf(0.0), Double.valueOf(fitEllipse.angle));
    assertEquals(Double.valueOf(5), Double.valueOf(fitEllipse.boundingRect().height));
    assertEquals(Integer.valueOf(3), Integer.valueOf(fitEllipse.boundingRect().width));
    assertEquals(Double.valueOf(15), Double.valueOf(fitEllipse.boundingRect().area()));

    // Draw the roi and the min circle
    Mat rgb = Mat.zeros(20, 20, CvType.CV_8UC3);
    roi.setRegion(contour);
    Lungs.paintROI(rgb, roi, ColourBGR.RED);
    Imgproc.ellipse(rgb, fitEllipse, new Scalar(ColourBGR.BLUE));

    // Uncomment to view roi and minimum bounding circle
    // new MatViewer(rgb).display();
  }
}
