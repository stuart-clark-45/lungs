package ml.feature;

import static ml.feature.LTP.GTE;
import static ml.feature.LTP.LT;
import static ml.feature.LTP.NUM_POS_VAL;
import static ml.feature.LTP.VOID;
import static org.apache.commons.lang.ArrayUtils.toObject;
import static org.junit.Assert.assertArrayEquals;
import static util.MatUtils.put;

import java.util.ArrayList;
import java.util.List;

import model.ROIAreaStats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.Histogram;
import model.ROI;
import util.MongoHelper;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class LTPTest {

  @Before
  public void setUp() throws Exception {
    Testing.drop();

    // Add some rois that are used to compute ROIAreaStats
    ROI roi = new ROI();
    roi.setArea(8);
    MongoHelper.getDataStore().save(roi);
    roi = new ROI();
    roi.setArea(2);
    MongoHelper.getDataStore().save(roi);

    ROIAreaStats.compute();
  }

  @After
  public void tearDown() throws Exception {
    Testing.drop();
  }

  @Test
  public void test() throws Exception {
    Mat mat = Mat.zeros(4, 4, CvType.CV_8UC1);

    List<Point> points = new ArrayList<>();
    Point point;

    point = new Point(2, 0);
    points.add(point);
    put(mat, point, 100);

    point = new Point(2, 1);
    points.add(point);
    put(mat, point, 30);

    point = new Point(1, 2);
    points.add(point);
    put(mat, point, 110);

    point = new Point(2, 2);
    points.add(point);
    put(mat, point, 30);

    point = new Point(3, 2);
    points.add(point);
    put(mat, point, 200);

    point = new Point(2, 3);
    points.add(point);
    put(mat, point, 220);

    ROI roi = new ROI();
    roi.setRegion(points);

    new LTP().compute(roi, mat);

    // Order is: Up Left, Up, Up Right, Right, Down Right, Down, Down Left, Left
    List<int[]> ltps = new ArrayList<>();
    ltps.add(new int[] {VOID, VOID, VOID, VOID, VOID, LT, VOID, VOID});
    ltps.add(new int[] {VOID, GTE, VOID, VOID, GTE, GTE, GTE, VOID});
    ltps.add(new int[] {VOID, VOID, LT, LT, GTE, VOID, VOID, VOID});
    ltps.add(new int[] {VOID, GTE, VOID, GTE, VOID, GTE, VOID, GTE});
    ltps.add(new int[] {LT, VOID, VOID, VOID, VOID, VOID, GTE, LT});
    ltps.add(new int[] {LT, LT, LT, VOID, VOID, VOID, VOID, VOID});

    List<Integer> values = new ArrayList<>(ltps.size());
    for (int[] lqp : ltps) {
      int value = 0;
      for (int i = 0; i < lqp.length; i++) {
        value += Math.pow(LTP.BASE, i) * lqp[i];
      }
      values.add(value);
    }

    Histogram coarse = new Histogram(LTP.getCoarse(), NUM_POS_VAL);
    values.forEach(coarse::add);
    coarse.computeBins();
    coarse.toFrequencies();
    assertArrayEquals(toObject(coarse.getBins()), toObject(roi.getLtpCoarse().getBins()));

    Histogram fine = new Histogram(LTP.getFine(), coarse);
    fine.computeBins();
    fine.toFrequencies();
    assertArrayEquals(toObject(fine.getBins()), toObject(roi.getLtpFine().getBins()));
  }

}
