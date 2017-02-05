package util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class IterableMatTest {

  @Test
  public void test() throws Exception {
    Mat mat = Imgcodecs.imread("./testres/segmented.bmp");

    List<Double> expected = new ArrayList<>();
    for (int row = 0; row < mat.rows(); row++) {
      for (int col = 0; col < mat.cols(); col++) {
        expected.add(mat.get(row, col)[0]);
      }
    }

    List<Double> actual = new ArrayList<>();
    for (Double[] val : new IterableMat(mat)) {
      actual.add(val[0]);
    }

    assertEquals(expected, actual);
  }

}
