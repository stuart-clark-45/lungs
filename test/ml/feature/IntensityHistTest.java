package ml.feature;

import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import ij.plugin.DICOM;
import model.roi.Histogram;
import model.roi.ROI;
import util.LungsException;
import util.MatUtils;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class IntensityHistTest {

  private static Mat MAT;
  private ROI roi;

  static {
    DICOM dicom = new DICOM();
    dicom.open("testres/test.dcm");
    MAT = MatUtils.fromDICOM(dicom);
  }

  @Before
  public void setUp() throws Exception {
    roi = new ROI();
    List<Point> region = new ArrayList<>();
    for (int row = 0; row < MAT.rows(); row++) {
      for (int col = 0; col < MAT.cols(); col++) {
        region.add(new Point(col, row));
      }
    }
    roi.setRegion(region);
  }

  @Test
  public void testCoarse() throws Exception {
    new CoarseHist().compute(roi, MAT);
    Histogram hist = roi.getCoarseHist();
    assertNotNull(hist);
    assertEquals(Integer.valueOf(CoarseHist.BINS), Integer.valueOf(hist.numBins()));
  }

  @Test
  public void testMid() throws Exception {
    new MedHist().compute(roi, MAT);
    Histogram hist = roi.getMedHist();
    assertNotNull(hist);
    assertEquals(Integer.valueOf(MedHist.BINS), Integer.valueOf(hist.numBins()));
  }

  @Test
  public void testFine() throws Exception {
    new FineHist().compute(roi, MAT);
    Histogram hist = roi.getFineHist();
    assertNotNull(hist);
    assertEquals(Integer.valueOf(FineHist.BINS), Integer.valueOf(hist.numBins()));
  }

  @Test
  public void testHistCorrect() throws Exception {
    Mat mat = Mat.zeros(10, 10, CvType.CV_8UC1);
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

    roi.setRegion(region);

    TestHist testHist = new TestHist();
    testHist.compute(roi, mat);

    double[] expected = {0.25, 0.375, 0.125, 0.25};
    assertArrayEquals(toObject(expected), toObject(testHist.histogram.getBins()));
  }

  private static class TestHist extends IntensityHist {

    private Histogram histogram;

    public TestHist() {
      super(4);
    }

    @Override
    public void compute(ROI roi, Mat mat) throws LungsException {
      histogram = createHist(roi, mat);
    }

  }
}
