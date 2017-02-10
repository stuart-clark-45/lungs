package ml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import model.ROI;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class MeanIntensityTest {

  @Test
  public void test() throws Exception {
    ROI roi = new ROI();
    roi.addPoint(new Point(0, 0));
    roi.addPoint(new Point(0, 1));
    roi.addPoint(new Point(0, 2));
    roi.addPoint(new Point(0, 3));
    roi.addPoint(new Point(1, 0));
    roi.addPoint(new Point(1, 1));
    roi.addPoint(new Point(1, 2));
    roi.addPoint(new Point(1, 3));

    Mat mat = Imgcodecs.imread("./testres/quarter-white.bmp", Imgcodecs.IMREAD_GRAYSCALE);

    new MeanIntensity().compute(roi, mat);

    assertEquals(Double.valueOf(255.0 / 2), roi.getMeanIntensity());
  }

}
