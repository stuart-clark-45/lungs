package vision;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import model.MinMax;
import util.MatUtils;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class BandThresholdTest {

  private static final double FOREGROUND = 225;

  @Test
  public void test() throws Exception {

    Mat input = Mat.zeros(2, 2, CvType.CV_8UC1);
    input.put(0, 0, 6);
    input.put(0, 1, 21);
    input.put(1, 0, 0);
    input.put(1, 1, 200);

    MinMax<Integer> band1 = new MinMax<>(5, 10);
    MinMax<Integer> band2 = new MinMax<>(20, 40);
    Mat thresholded = new BandThreshold(FOREGROUND, Arrays.asList(band1, band2)).threshold(input);

    Mat expected = MatUtils.similarMat(input);
    expected.put(0, 0, FOREGROUND);
    expected.put(0, 1, FOREGROUND);

    for (int row = 0; row < input.rows(); row++) {
      for (int col = 0; col < input.cols(); col++) {
        assertEquals(expected.get(row, col)[0], thresholded.get(row, col)[0], 0.0);
      }
    }

  }
}
