package ml.feature;

import static ml.feature.LTP.BINS;
import static ml.feature.LTP.GTE;
import static ml.feature.LTP.LT;
import static ml.feature.LTP.NUM_POS_VAL;
import static ml.feature.LTP.VOID;
import static org.apache.commons.lang.ArrayUtils.toObject;
import static org.junit.Assert.assertArrayEquals;
import static util.MatUtils.put;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.Histogram;
import model.ROI;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class LTPTest {

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
    List<int[]> lqps = new ArrayList<>();
    lqps.add(new int[] {VOID, VOID, VOID, VOID, VOID, LT, VOID, VOID});
    lqps.add(new int[] {VOID, GTE, VOID, VOID, GTE, GTE, GTE, VOID});
    lqps.add(new int[] {VOID, VOID, LT, LT, GTE, VOID, VOID, VOID});
    lqps.add(new int[] {VOID, GTE, VOID, GTE, VOID, GTE, VOID, GTE});
    lqps.add(new int[] {LT, VOID, VOID, VOID, VOID, VOID, GTE, LT});
    lqps.add(new int[] {LT, LT, LT, VOID, VOID, VOID, VOID, VOID});

    List<Integer> values = new ArrayList<>(lqps.size());
    for (int[] lqp : lqps) {
      int value = 0;
      for (int i = 0; i < lqp.length; i++) {
        value += Math.pow(LTP.BASE, i) * lqp[i];
      }
      values.add(value);
    }

    Histogram histogram = new Histogram(BINS, NUM_POS_VAL);
    values.forEach(histogram::add);
    histogram.computeBins();
    histogram.toFrequencies();

    assertArrayEquals(toObject(histogram.getBins()), toObject(roi.getLtp().getBins()));
  }

}
