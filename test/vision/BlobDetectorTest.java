package vision;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import model.KeyPoint;
import util.ColourBGR;
import util.MatViewer;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class BlobDetectorTest {

  @Test
  public void test() throws Exception {
    BlobDetector detector = new BlobDetector(40, 20);

    Mat mat = Imgcodecs.imread(getClass().getResource("/blobs.bmp").getPath());
    Mat grey = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
    Imgproc.cvtColor(mat, grey, Imgproc.COLOR_BGR2GRAY);

    List<KeyPoint> keyPoints = detector.detect(grey, null);

    for (KeyPoint keyPoint : keyPoints) {
      Imgproc.circle(mat, keyPoint.getPoint(), (int) keyPoint.getRadius(),
          new Scalar(ColourBGR.RED), 1);
    }

    // Uncomment to view result
//    new MatViewer(grey, mat).display();
  }

}
