package ml.feature;

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
  public void test() throws Exception {
    new AllHists().compute(roi, MAT);
    Histogram coarse = roi.getCoarseHist();
    assertNotNull(coarse);
    assertEquals(Integer.valueOf(AllHists.COARSE), Integer.valueOf(coarse.getNumBins()));

    Histogram mid = roi.getMedHist();
    assertNotNull(mid);
    assertEquals(Integer.valueOf(AllHists.MID), Integer.valueOf(mid.getNumBins()));

    Histogram fine = roi.getFineHist();
    assertNotNull(fine);
    assertEquals(Integer.valueOf(AllHists.FINE), Integer.valueOf(fine.getNumBins()));
  }

}
