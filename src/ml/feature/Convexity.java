package ml.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.ROI;
import util.LungsException;
import util.MatUtils;
import util.PointUtils;
import vision.ConvexHull;
import vision.ROIExtractor;

/**
 * Used to compute the value for {@link ROI#convexity}.
 *
 * @author Stuart Clark
 */
public class Convexity implements Feature {

  private static final Logger LOGGER = LoggerFactory.getLogger(Convexity.class);

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    List<Point> region = roi.getRegion();
    Mat minMat = PointUtils.points2MinMat(region, PointUtils.xyMaxMin(region), null);

    // Get the external contour of the ROI
    List<MatOfPoint> contours = new ArrayList<>();
    Imgproc.findContours(minMat, contours, new Mat(), Imgproc.RETR_EXTERNAL,
        Imgproc.CHAIN_APPROX_NONE);

    // Get the convex hull for the ROI (will only ever be one contour)
    MatOfPoint hull = ConvexHull.findHull(contours.get(0));

    // Draw the convex hull to a mat
    Mat hullsMat = MatUtils.similarMat(minMat, true);
    Imgproc.fillPoly(hullsMat, Collections.singletonList(hull), new Scalar(Lungs.FOREGROUND));

    // Extract the convex hull into an ROI
    Mat labels = MatUtils.similarMat(hullsMat, false);
    Imgproc.connectedComponents(hullsMat, labels);
    List<ROI> hullROIs = ROIExtractor.labelsToROIs(labels);
    int numHullROIs = hullROIs.size();
    if (numHullROIs != 1) {
      LOGGER.error("Convex hull returned " + numHullROIs
          + " instead of 1 will proceed using the first");
    }
    ROI hullROI = hullROIs.get(0);

    // Calculate and set convexity
    roi.setConvexity(region.size() / (double) hullROI.getRegion().size());
  }

}
