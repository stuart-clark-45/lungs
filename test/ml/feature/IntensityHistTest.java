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
import org.opencv.core.Mat;
import org.opencv.core.Point;

import ij.plugin.DICOM;
import model.Histogram;
import model.ROI;
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
    assertEquals(Integer.valueOf(CoarseHist.BINS), Integer.valueOf(hist.getNumBins()));
  }

  @Test
  public void testMid() throws Exception {
    new MedHist().compute(roi, MAT);
    Histogram hist = roi.getMedHist();
    assertNotNull(hist);
    assertEquals(Integer.valueOf(MedHist.BINS), Integer.valueOf(hist.getNumBins()));
  }

  @Test
  public void testFine() throws Exception {
    new FineHist().compute(roi, MAT);
    Histogram hist = roi.getFineHist();
    assertNotNull(hist);
    assertEquals(Integer.valueOf(FineHist.BINS), Integer.valueOf(hist.getNumBins()));
  }

}
