package vision;

import static org.opencv.imgproc.Imgproc.COLORMAP_HSV;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.applyColorMap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import util.MatUtils;

/**
 * Used segment CT slices.
 *
 * @author Stuart Clark
 */
public class SliceSegmenter {

  /**
   * The value used for the foreground in the segmented images that are returned.
   */
  private int foreground;

  private int sureFG;

  private int sureBG;

  public SliceSegmenter(int foreground, int sureFG, int sureBG) {
    this.foreground = foreground;
    this.sureFG = sureFG;
    this.sureBG = sureBG;
  }

  public Mat segment(Mat original) {
    Mat foregroundMat = MatUtils.similarMat(original);
    Imgproc.threshold(original, foregroundMat, sureFG, foreground, THRESH_BINARY);

    Mat backgroundMat = MatUtils.similarMat(original);
    Imgproc.threshold(original, backgroundMat, sureBG, foreground, THRESH_BINARY);

    Mat unknownMat = MatUtils.similarMat(original);
    Core.subtract(backgroundMat, foregroundMat, unknownMat);

    // Temp label with become the wrong CvType when connected Components is called
    Mat labels = MatUtils.similarMat(original);
    Imgproc.connectedComponents(foregroundMat, labels);

    // Add one to each of the labels so that we can mark the unknown region with 0's
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        labels.put(row, col, labels.get(row, col)[0] + 1);
      }
    }

    // Mark the unknown region with 0's
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        if (unknownMat.get(row, col)[0] == foreground) {
          labels.put(row, col, 0);
        }
      }
    }

    Mat bgr = MatUtils.grey2BGR(original);
    Imgproc.watershed(bgr, labels);

    Mat thresholded = MatUtils.similarMat(original);
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        if (labels.get(row, col)[0] == -1) {
          thresholded.put(row, col, foreground);
        }
      }
    }

    return thresholded;
  }

}
