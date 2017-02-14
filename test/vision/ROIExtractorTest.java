package vision;

import static org.junit.Assert.assertEquals;
import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import model.ROI;
import util.MatUtils;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class ROIExtractorTest {

  private static final int FOREGROUND = 255;

  @Test
  public void test() throws Exception {
    Mat segmented = Imgcodecs.imread("./testres/segmented.bmp", CV_LOAD_IMAGE_GRAYSCALE);
    ROIExtractor extractor = new ROIExtractor(FOREGROUND);

    List<ROI> rois = extractor.extract(segmented);

    assertEquals(5, rois.size());

    // Reconstruct the segmented mat using the RIOs
    Mat rebuilt = MatUtils.similarMat(segmented);
    for (ROI roi : rois) {
      for (Point point : roi.getRegion()) {
        rebuilt.put((int) point.y, (int) point.x, (double) FOREGROUND);
      }
    }

    // Check the reconstructed mat is the same as the original
    for (int row = 0; row < segmented.rows(); row++) {
      for (int col = 0; col < segmented.cols(); col++) {
        assertEquals(Double.valueOf(segmented.get(row, col)[0]),
            Double.valueOf(rebuilt.get(row, col)[0]));
      }
    }

  }
}
