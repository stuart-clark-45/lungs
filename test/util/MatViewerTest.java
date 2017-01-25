package util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Clark
 */
@RunWith(PreTest.class)
public class MatViewerTest {

  @Test
  public void test() throws Exception {
    List<Mat> mats = new ArrayList<>();

    mats.add(Imgcodecs.imread(getClass().getResource("/biggie.jpg").getPath()));
    mats.add(Imgcodecs.imread(getClass().getResource("/yellow-sub.jpg").getPath()));

    new MatViewer(mats).display();
  }

}
