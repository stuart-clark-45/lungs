package vision;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import model.KeyPoint;
import util.ColourBGR;
import util.MatUtils;
import util.MatViewer;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class BlobDetectorTest {

  @Test
  @Ignore
  public void test() throws Exception {
    BlobDetector detector = BlobDetector.getInstance();

    Mat mat = Imgcodecs.imread(getClass().getResource("/blobs.bmp").getPath());
    Mat annotated = MatUtils.grey2BGR(mat);


    List<KeyPoint> keyPoints = detector.detect(mat, null);

    for (KeyPoint keyPoint : keyPoints) {
      Imgproc.circle(annotated, keyPoint.getPoint(), (int) keyPoint.getRadius(), new Scalar(
          ColourBGR.RED), 1);
    }
    new MatViewer(mat, annotated).display();
  }

}
