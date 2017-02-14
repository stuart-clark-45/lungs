package vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import model.roi.ROI;
import util.LungsException;

/**
 * Used to extract {@link ROI}s from a {@link Mat} that has been segmented.
 *
 * @author Stuart Clark
 */
public class ROIExtractor {

  private static final int NOT_LABELLED = 0;
  private static final int LABELLED = 1;

  /**
   * The intensity value for pixels that are in the foreground.
   */
  private int foreground;

  /**
   * @param foreground he intensity value for pixels that are in the foreground.
   */
  public ROIExtractor(int foreground) {
    this.foreground = foreground;
  }

  /**
   * Returns a list of {@link ROI}s obtained from {@code segmented} using an implementation of the
   * connected-component labeling algorithm.
   *
   * @param segmented
   * @return
   * @throws LungsException
   */
  public List<ROI> extract(Mat segmented) throws LungsException {
    if (segmented.channels() != 1) {
      throw new LungsException("The Mat must have only one channel");
    }

    // Return list
    List<ROI> rois = new ArrayList<>();

    // Mat used to record object labels
    Mat objects = Mat.zeros(segmented.rows(), segmented.cols(), CvType.CV_8UC1);

    // Label the objects in mat
    for (int row = 0; row < segmented.rows(); row++) {
      for (int col = 0; col < segmented.cols(); col++) {
        // If the pixel is white and has not been accepted as part of an ROI yet
        if (!isLabeled(objects, row, col) && isForeground(segmented, row, col)) {
          ROI roi = new ROI();
          labelPixel(segmented, objects, row, col, roi);
          rois.add(roi);
        }
      }
    }

    return rois;
  }

  /**
   * Returns a single {@link ROI}s obtained from {@code segmented} using an implementation of the
   * connected-component labeling algorithm.
   * 
   * @param segmented
   * @param seed the point to use as a seed for growing the {@link ROI}.
   * @return
   */
  public ROI extractOne(Mat segmented, Point seed) {
    ROI roi = new ROI();
    Mat objects = Mat.zeros(segmented.rows(), segmented.cols(), CvType.CV_8UC1);
    labelPixel(segmented, objects, (int) seed.y, (int) seed.x, roi);
    return roi;
  }

  private void labelPixel(Mat segmented, Mat objects, int row, int col, ROI roi) {
    // If the pixel is white and has not been accepted as part of an ROI yet
    if (!isLabeled(objects, row, col) && isForeground(segmented, row, col)) {
      roi.addPoint(new Point(col, row));
      objects.put(row, col, LABELLED);

      // Label pixel up and left from current
      if (row - 1 > -1 && col - 1 > -1) {
        labelPixel(segmented, objects, row - 1, col - 1, roi);
      }

      // Label pixel up from current
      if (col - 1 > -1) {
        labelPixel(segmented, objects, row, col - 1, roi);
      }

      // Label pixel up and right from current
      if (row + 1 < segmented.rows() && col - 1 > -1) {
        labelPixel(segmented, objects, row + 1, col - 1, roi);
      }

      // Label pixel right from current
      if (row + 1 < segmented.rows()) {
        labelPixel(segmented, objects, row + 1, col, roi);
      }

      // Label pixel down and right from current
      if (row + 1 < segmented.rows() && col + 1 < segmented.cols()) {
        labelPixel(segmented, objects, row + 1, col + 1, roi);
      }

      // Label pixel down from current
      if (col + 1 < segmented.cols()) {
        labelPixel(segmented, objects, row, col + 1, roi);
      }

      // Label pixel left and down from current
      if (row - 1 > -1 && col + 1 < segmented.cols()) {
        labelPixel(segmented, objects, row - 1, col + 1, roi);
      }

      // Label pixel left from current
      if (row - 1 > -1) {
        labelPixel(segmented, objects, row - 1, col, roi);
      }

    }
  }

  /**
   * @param objects the {@link Mat} that holds the labels for the pixels.
   * @param row
   * @param col
   * @return true if the pixel at (row, col) has been assigned a label, false otherwise.
   */
  private boolean isLabeled(Mat objects, int row, int col) {
    return objects.get(row, col)[0] != NOT_LABELLED;
  }

  /**
   * @param segmented a grey-scale segmented image.
   * @param row
   * @param col
   * @return true if the pixel at (row, col) is in the foreground, false otherwise.
   */
  private boolean isForeground(Mat segmented, int row, int col) {
    // Assumed that segmented has one channel and the foreground has maximum possible value.
    return segmented.get(row, col)[0] == foreground;
  }

}
