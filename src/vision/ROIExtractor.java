package vision;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import model.ROI;
import util.MatUtils;

/**
 * Used extract ROIs from {@link Mat}s using a combination of the watershed algorithm and the
 * connected component algorithm.
 *
 * @author Stuart Clark
 */
public class ROIExtractor {

  /**
   * The value used for the foreground in the segmented images that are returned.
   */
  private int foreground;

  private int sureFG;

  private int sureBG;

  public ROIExtractor(int foreground, int sureFG, int sureBG) {
    this.foreground = foreground;
    this.sureFG = sureFG;
    this.sureBG = sureBG;
  }

  public List<ROI> extractROIs(Mat original) {
    // Apply threshold to find the sure foreground
    Mat foregroundMat = MatUtils.similarMat(original);
    Imgproc.threshold(original, foregroundMat, sureFG, foreground, THRESH_BINARY);

    // Apply threshold to find the sure background
    Mat backgroundMat = MatUtils.similarMat(original);
    Imgproc.threshold(original, backgroundMat, sureBG, foreground, THRESH_BINARY);

    // Subtract the sure foreground from the sure background to find the unknown region
    Mat unknownMat = MatUtils.similarMat(original);
    Core.subtract(backgroundMat, foregroundMat, unknownMat);

    // Label the connected components found in the sure forground
    Mat labels = MatUtils.similarMat(original);
    Imgproc.connectedComponents(foregroundMat, labels);

    // Add one to each of the labels so that we can mark the unknown region with 0's
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        labels.put(row, col, labels.get(row, col)[0] + 1);
      }
    }

    // Mark the unknown region with 0's so that labels can be used by Imgproc.watershed(..)
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        if (unknownMat.get(row, col)[0] == foreground) {
          labels.put(row, col, 0);
        }
      }
    }

    // Run the watershed algorithm (this will update labels)
    Mat bgr = MatUtils.grey2BGR(original);
    Imgproc.watershed(bgr, labels);

    // Extract all the rois. id starts from 1 because we want to ignore boundaries (-1) and the
    // background (0)
    int id = 1;
    List<ROI> rois = new ArrayList<>();
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        if (labels.get(row, col)[0] == id) {
          ROI roi = new ROI();
          populateROI(row, col, labels, id, roi);
          rois.add(roi);
          id++;
        }
      }
    }

    return rois;
  }

  /**
   * Recursive method used to populate the {@link ROI#region}.
   * 
   * @param row the row of the current pixel being examined.
   * @param col the column of the current pixel being examined.
   * @param labels the {@link Mat} containing the labels.
   * @param id the id for the roi that is being extracted.
   * @param roi the {@link ROI} to populate.
   */
  private void populateROI(int row, int col, Mat labels, int id, ROI roi) {
    // If the pixel is white and has not been accepted as part of an ROI yet
    if (labels.get(row, col)[0] == id) {
      roi.addPoint(new Point(col, row));
      // By setting this pixel to -2 we can show that it has already been visited and extracted into
      // an ROI. This avoids duplicate pints in the region lists.
      labels.put(row, col, -2);

      // Label pixel up and left from current
      if (row - 1 > -1 && col - 1 > -1) {
        populateROI(row - 1, col - 1, labels, id, roi);
      }

      // Label pixel up from current
      if (col - 1 > -1) {
        populateROI(row, col - 1, labels, id, roi);
      }

      // Label pixel up and right from current
      if (row + 1 < labels.rows() && col - 1 > -1) {
        populateROI(row + 1, col - 1, labels, id, roi);
      }

      // Label pixel right from current
      if (row + 1 < labels.rows()) {
        populateROI(row + 1, col, labels, id, roi);
      }

      // Label pixel down and right from current
      if (row + 1 < labels.rows() && col + 1 < labels.cols()) {
        populateROI(row + 1, col + 1, labels, id, roi);
      }

      // Label pixel down from current
      if (col + 1 < labels.cols()) {
        populateROI(row, col + 1, labels, id, roi);
      }

      // Label pixel left and down from current
      if (row - 1 > -1 && col + 1 < labels.cols()) {
        populateROI(row - 1, col + 1, labels, id, roi);
      }

      // Label pixel left from current
      if (row - 1 > -1) {
        populateROI(row - 1, col, labels, id, roi);
      }

    }
  }

}
