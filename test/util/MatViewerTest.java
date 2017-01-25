package util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

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


    mats.add(Highgui.imread(getClass().getResource("/biggie.jpg").getPath()));
    mats.add(Highgui.imread(getClass().getResource("/yellow-sub.jpg").getPath()));

    new MatViewer(mats).display();
  }

}
