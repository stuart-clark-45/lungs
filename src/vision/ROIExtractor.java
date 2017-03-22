package vision;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   * The value used for the foreground in the segmented images.
   */
  private static final int FOREGROUND = 255;

  /**
   * The label given to boundaries by {@link Imgproc#watershed(Mat, Mat)}.
   */
  private static final double BOUNDARIES = -1;

  /**
   * The label given to pixels that have been extracted into an {@link ROI} by
   * {@link ROIExtractor#populateROI(int, int, Mat, double, ROI)}.
   */
  private static final double EXTRACTED = -2;

  /**
   * The label given to pixels that are part of the background.
   */
  private static final double BACKGROUND = 0;

  /**
   * The threshold value that when used returns a thresholded image where the foreground is the
   * pixels of the original image that are known to be in the foreground of the original image
   */
  private int sureFG;

  /**
   * The threshold value that when used returns a thresholded image where the background is the
   * pixels of the original image that are known to be in the background of the original image
   */
  private int sureBG;

  /**
   * @param sureFG The threshold value that when used returns a thresholded image where the
   *        foreground is the pixels of the original image that are known to be in the foreground of
   *        the original image
   * @param sureBG The threshold value that when used returns a thresholded image where the
   *        background is the pixels of the original image that are known to be in the background of
   *        the original image
   */
  public ROIExtractor(int sureFG, int sureBG) {
    this.sureFG = sureFG;
    this.sureBG = sureBG;
  }

  public List<ROI> extractROIs(Mat original) {
    // Apply threshold to find the sure foreground
    Mat foregroundMat = MatUtils.similarMat(original, false);
    Imgproc.threshold(original, foregroundMat, sureFG, FOREGROUND, THRESH_BINARY);

    // Apply threshold to find the sure background
    Mat backgroundMat = MatUtils.similarMat(original, false);
    Imgproc.threshold(original, backgroundMat, sureBG, FOREGROUND, THRESH_BINARY);

    // Subtract the sure foreground from the sure background to find the unknown region
    Mat unknownMat = MatUtils.similarMat(original, false);
    Core.subtract(backgroundMat, foregroundMat, unknownMat);

    // Label the connected components found in the sure foreground
    Mat labels = MatUtils.similarMat(original, false);
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
        if (unknownMat.get(row, col)[0] == FOREGROUND) {
          labels.put(row, col, 0);
        }
      }
    }

    // Run the watershed algorithm (this will update labels)
    Mat bgr = MatUtils.grey2BGR(original);
    Imgproc.watershed(bgr, labels);


    return labelsToROIs(labels);
  }

  /**
   * Extract all the rois. id starts from 1 because we want to ignore boundaries (-1) and the
   * background (0)
   *
   * @param labels a {@link Mat} containing labels created using the connected component algorithm.
   * @return the rois.
   */
  public static List<ROI> labelsToROIs(Mat labels) {
    Set<Double> ignoreIds = new HashSet<>();
    ignoreIds.add(BOUNDARIES);
    ignoreIds.add(EXTRACTED);
    ignoreIds.add(BACKGROUND);
    List<ROI> rois = new ArrayList<>();
    for (int row = 0; row < labels.rows(); row++) {
      for (int col = 0; col < labels.cols(); col++) {
        double id = labels.get(row, col)[0];
        if (!ignoreIds.contains(id)) {
          ROI roi = new ROI();
          populateROI(row, col, labels, id, roi);
          rois.add(roi);
          ignoreIds.add(id);
        }
      }
    }

    return rois;
  }

  /**
   * @param labels a {@link Mat} with labeled connected components.
   * @param point one of the points in the connected component that should be extracted into an ROI.
   * @return the {@link ROI} for the connected component that has a pixel at {@code point}.
   */
  public static ROI extractOne(Mat labels, Point point) {
    ROI roi = new ROI();
    int row = (int) point.y;
    int col = (int) point.x;
    double id = labels.get(row, col)[0];
    populateROI(row, col, labels, id, roi);
    return roi;
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
  private static void populateROI(int row, int col, Mat labels, double id, ROI roi) {
    // If the pixel is white and has not been accepted as part of an ROI yet
    if (labels.get(row, col)[0] == id) {
      roi.addPoint(new Point(col, row));
      // By setting this pixel to -2 we can show that it has already been visited and extracted into
      // an ROI. This avoids duplicate pints in the region lists.
      labels.put(row, col, EXTRACTED);

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
