package vision;

import org.opencv.core.CvType;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

/**
 * Used to find the convex hull of a contour.
 *
 * @author Stuart Clark
 */
public class ConvexHull {

  private ConvexHull() {
    // Hide the constructor
  }

  /**
   * @param contour
   * @return the convex hull for the {@code contour}.
   */
  public static MatOfPoint findHull(MatOfPoint contour) {
    // Find the convex hull for the contour
    MatOfInt hull = new MatOfInt();
    Imgproc.convexHull(contour, hull);

    // Convert MatOfInt to MatOfPoint
    MatOfPoint matOfPoint = new MatOfPoint();
    matOfPoint.create((int) hull.size().height, 1, CvType.CV_32SC2);
    for (int i = 0; i < hull.size().height; i++) {
      int index = (int) hull.get(i, 0)[0];
      double[] point = new double[] {contour.get(index, 0)[0], contour.get(index, 0)[1]};
      matOfPoint.put(i, 0, point);
    }

    return matOfPoint;
  }

}
