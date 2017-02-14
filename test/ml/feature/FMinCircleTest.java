package ml.feature;

import static junit.framework.TestCase.assertEquals;

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
import model.roi.ROI;
import util.ColourBGR;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class RadiusTest {

  @Test
  public void test() throws Exception {
    List<Point> contour = new ArrayList<>();
    contour.add(new Point(4, 5));
    contour.add(new Point(4, 6));
    contour.add(new Point(4, 7));
    contour.add(new Point(4, 8));
    contour.add(new Point(4, 9));

    ROI roi = new ROI();
    roi.setContour(contour);
    roi.setRegion(contour);

    Radius radius = new Radius();
    radius.compute(roi, null);
    assertEquals(2.0001f, roi.getRadius());
    assertEquals(new Point(4, 7), radius.lastCenter);

    Mat rgb = Mat.zeros(20, 20, CvType.CV_8UC3);
    new Lungs().paintROI(rgb, roi, ColourBGR.RED);
    Imgproc.circle(rgb, radius.lastCenter, (int) roi.getRadius(), new Scalar(ColourBGR.BLUE));

    // Uncomment to view roi and minimum bounding circle
    // new MatViewer(rgb).display();
  }

}
