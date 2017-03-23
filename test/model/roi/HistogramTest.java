package model.roi;

import static model.Histogram.POS_VALS_8BIT;
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.Histogram;
import model.ROI;
import util.LungsException;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class HistogramTest {

  private Mat mat;

  @Before
  public void setUp() throws Exception {
    mat = Mat.zeros(10, 10, CvType.CV_8UC1);
    mat.put(1, 1, 255);
    mat.put(1, 2, 255);
    mat.put(1, 3, 255);
    mat.put(1, 4, 255);

    mat.put(2, 1, 129);
    mat.put(2, 2, 129);
    mat.put(2, 3, 120);
    mat.put(2, 4, 120);

    mat.put(3, 1, 120);
    mat.put(3, 2, 120);
    mat.put(3, 3, 120);
    mat.put(3, 4, 120);
  }

  @Test(expected = LungsException.class)
  public void testBadMat() throws Exception {
    Histogram histogram = new Histogram(POS_VALS_8BIT);
    histogram.createHist(Mat.zeros(1, 1, CvType.CV_8UC3), 2);
  }

  @Test(expected = LungsException.class)
  public void testBadBins() throws Exception {
    Histogram histogram = new Histogram(POS_VALS_8BIT);
    histogram.createHist(Mat.zeros(1, 1, CvType.CV_16UC1), 13);
  }

  @Test
  public void testWithROI() throws Exception {
    List<Point> region = new ArrayList<>();
    region.add(new Point(1, 1));
    region.add(new Point(2, 1));
    region.add(new Point(3, 1));
    region.add(new Point(4, 1));

    region.add(new Point(1, 2));
    region.add(new Point(2, 2));
    region.add(new Point(3, 2));
    region.add(new Point(4, 2));

    region.add(new Point(1, 3));
    region.add(new Point(2, 3));
    region.add(new Point(3, 3));
    region.add(new Point(4, 3));

    region.add(new Point(1, 4));
    region.add(new Point(2, 4));
    region.add(new Point(3, 4));
    region.add(new Point(4, 4));

    ROI roi = new ROI();
    roi.setRegion(region);

    Histogram histogram = new Histogram(POS_VALS_8BIT);
    histogram.createHist(roi.getRegion(), mat, 4);

    double[] expected = {0.25, 0.375, 0.125, 0.25};
    assertArrayEquals(toObject(expected), toObject(histogram.getBins()));
  }

  @Test
  public void testWithOutROI() throws Exception {
    Histogram histogram = new Histogram(POS_VALS_8BIT);
    histogram.createHist(mat, 4);
    double[] expected = {0.88, 0.06, 0.02, 0.04};
    assertArrayEquals(toObject(expected), toObject(histogram.getBins()));
  }

}
