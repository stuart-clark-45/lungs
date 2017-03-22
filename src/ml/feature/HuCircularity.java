package ml.feature;

import static java.lang.Math.PI;
import static java.lang.Math.pow;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import model.ROI;
import util.LungsException;
import util.PointUtils;

/**
 * Used to compute the value for {@link ROI#huCircularity} using moments.
 *
 * @author Stuart Clark
 */
public class HuCircularity implements Feature {

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    List<Point> region = roi.getRegion();
    Mat minMat = PointUtils.points2MinMat(region, PointUtils.xyMaxMin(region), null);
    Moments moments = Imgproc.moments(minMat, true);
    double circularity =
        1 / (2 * PI) * (pow(moments.get_m00(), 2) / (moments.get_m20() + moments.get_m02()));
    roi.setHuCircularity(circularity);
  }
}
