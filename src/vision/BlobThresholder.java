package vision;

import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import core.Lungs;
import model.MinMaxXY;
import model.ROI;
import util.LungsException;
import util.MatUtils;
import util.PointUtils;

/**
 * Used to threshold a blob obtained using {@link Lungs#extractJuxtapleural(Mat, List, Mat)}.
 *
 * @author Stuart Clark
 */
public class BlobThresholder {

  /**
   * The number of times that {@link this#thresholdBlob(ROI, Mat)} has been called successfully.
   */
  private double success;

  /**
   * The number of times that {@link this#thresholdBlob(ROI, Mat)} has been called and failed.
   */
  private double failure;

  /**
   *
   * @param blob
   * @param original
   * @return
   * @throws LungsException
   */
  public ROI thresholdBlob(ROI blob, Mat original) throws LungsException {
    List<Point> blobRegion = blob.getRegion();

    // Get min and max x and y co-ordinates
    MinMaxXY<Double> mmXY = PointUtils.xyMaxMin(blobRegion);

    double blobRadius = (mmXY.maxX - mmXY.minX) / 2;

    // Half the length of one of the edges of a bounding box that has the 2 times the area of the
    // blob
    double boxRadius = sqrt(2 * PI * pow(blobRadius, 2)) / 2;

    // Adjust mmXY so that the min mat is for the box not the blob
    int diff = (int) Math.round(boxRadius - blobRadius);
    MinMaxXY<Integer> rounded = round(mmXY);
    rounded.minY -= diff;
    rounded.maxY += diff;
    rounded.minX -= diff;
    rounded.maxX += diff;
    clip(rounded, original);

    // Create a sub mat using the bounding box
    Mat submat = original.submat(rounded.minY, rounded.maxY, rounded.minX, rounded.maxX);

    // Threshold the submat
    Mat thresholded = MatUtils.similarMat(submat, false);
    Imgproc.threshold(submat, thresholded, -1, Lungs.FOREGROUND, THRESH_OTSU);

    // Extract the ROIs from thresholded
    Mat labels = MatUtils.similarMat(thresholded, false);
    Imgproc.connectedComponents(thresholded, labels);
    List<ROI> rois = ROIExtractor.labelsToROIs(labels);

    // Get the ROI which has points in the blob
    ROI blobROI;
    try {
      blobROI = getBlobROI(rois, blobRegion, rounded);
    } catch (LungsException e) {
      failure++;
      throw e;
    }

    // Set the blob back in the thresholded submat
    for (Point point : blobRegion) {
      thresholded.put((int) point.y - rounded.minY, (int) point.x - rounded.minX, 0);
    }

    // Get ROIs from the thresholded submat with the blob painted black
    Imgproc.connectedComponents(thresholded, labels);
    rois = ROIExtractor.labelsToROIs(labels);

    // Get ROIs that are touching the side of the bounding box
    List<ROI> touchingSide =
        rois.stream()
            .filter(
                roi -> {
                  for (Point point : roi.getRegion()) {
                    boolean isTouchingSide =
                        point.x == 0 || point.x == thresholded.cols() - 1 || point.y == 0
                            || point.y == thresholded.rows() - 1;
                    if (isTouchingSide) {
                      return true;
                    }
                  }

                  return false;
                }).collect(Collectors.toList());

    // Remove all the points contained in ROIs that touch the side of the bounding box from the
    // blobROI
    HashSet<Point> blobROIPoints = new HashSet<>(blobROI.getRegion());
    touchingSide.forEach(roi -> blobROIPoints.removeAll(roi.getRegion()));
    blobROI.setRegion(new ArrayList<>(blobROIPoints));

    // Add the offsets back onto the blobROI points
    for (Point point : blobROI.getRegion()) {
      point.x += rounded.minX;
      point.y += rounded.minY;
    }

    success++;

    return blobROI;
  }

  /**
   * @param mmXY
   * @return an integer version of {@code mmXY} that has values rounded to the nearest integer.
   */
  private MinMaxXY<Integer> round(MinMaxXY<Double> mmXY) {
    MinMaxXY<Integer> rounded = new MinMaxXY<>();
    rounded.minY = (int) Math.round(mmXY.minY);
    rounded.maxY = (int) Math.round(mmXY.maxY);
    rounded.minX = (int) Math.round(mmXY.minX);
    rounded.maxX = (int) Math.round(mmXY.maxX);

    return rounded;
  }

  /**
   * Adjusts the values of {@code mmXY} to unsure that none of them are outside the bounds of
   * {@code original}.
   *
   * @param maxXY
   * @param original
   */
  private void clip(MinMaxXY<Integer> maxXY, Mat original) {
    if (maxXY.minY < 0) {
      maxXY.minY = 0;
    }

    if (maxXY.maxY >= original.rows()) {
      maxXY.maxY = original.rows() - 1;
    }

    if (maxXY.minX < 0) {
      maxXY.minX = 0;
    }

    if (maxXY.maxX >= original.cols()) {
      maxXY.maxX = original.cols() - 1;
    }
  }

  /**
   * @param rois list of candidate {@link ROI}s.
   * @param blobRegion list of points that are within the blob.
   * @param rounded the rounded offsets for the roi co-ordinates.
   * @return the {@link ROI} that (partially) fills the blob.
   * @throws LungsException if no single blob could be identified.
   */
  private ROI getBlobROI(List<ROI> rois, List<Point> blobRegion, MinMaxXY<Integer> rounded)
      throws LungsException {

    // Get all the thresholded ROIs that have at least one point inside the blob
    List<ROI> blobROIs = rois.stream().filter(roi -> {
      for (Point point : roi.getRegion()) {
        if (blobRegion.contains(new Point(point.x + rounded.minX, point.y + rounded.minY))) {
          return true;
        }
      }
      return false;
    }).collect(Collectors.toList());

    int numBlobROIs = blobROIs.size();

    // Should only ever be one if thresholding is working properly
    if (numBlobROIs == 1) {
      return blobROIs.get(0);
    } else {
      throw new LungsException(numBlobROIs + " ROIs found inside the blob there should only be one");
    }
  }

  /**
   * @return the success rate of {@code this}.
   */
  public double successRate() {
    return success / (failure + success);
  }

}
