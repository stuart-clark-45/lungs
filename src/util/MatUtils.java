package util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.stream.Collectors;

import config.Misc;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.plugin.DICOM;
import model.CTSlice;
import model.CTStack;

/**
 * Suite of utility methods that can be used to process {@link Mat}s.
 *
 * @author Stuart Clark
 */
public class MatUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatUtils.class);
  private static final String IMAGE_DIR = ConfigHelper.getString(Misc.LIDC) + "/DOI";

  private MatUtils() {
    // Hide constructor
  }

  /**
   * Convert {@code mat} into a {@link BufferedImage} and return it.
   *
   * @param mat
   * @return
   */
  public static BufferedImage toBufferedImage(Mat mat) {
    int type = BufferedImage.TYPE_BYTE_GRAY;
    if (mat.channels() > 1) {
      type = BufferedImage.TYPE_3BYTE_BGR;
    }
    int bufferSize = mat.channels() * mat.cols() * mat.rows();
    byte[] b = new byte[bufferSize];
    mat.get(0, 0, b); // get all the pixels
    BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    System.arraycopy(b, 0, targetPixels, 0, b.length);
    return image;
  }

  /**
   * @param dicom
   * @return a {@link Mat} read from {@code dicom}.
   */
  public static Mat fromDICOM(DICOM dicom) {
    BufferedImage bi = dicom.getBufferedImage();
    Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
    byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
    mat.put(0, 0, data);
    return mat;
  }

  /**
   * @param mats single channel {@link Mat}s.
   * @return {@code mats} converted to an BGR {@link Mat}s.
   */
  public static List<Mat> grey2BGR(List<Mat> mats) {
    return mats.parallelStream().map(MatUtils::grey2BGR).collect(Collectors.toList());
  }

  /**
   * @param mat a single channel {@link Mat}.
   * @return mat converted to an BGR mat
   */
  public static Mat grey2BGR(Mat mat) {
    Mat bgr = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC3);

    for (int i = 0; i < mat.rows(); i++) {
      for (int j = 0; j < mat.cols(); j++) {
        double val = mat.get(i, j)[0];
        bgr.put(i, j, val, val, val);
      }
    }

    return bgr;
  }

  /**
   * Wrapper method that allows easy acces to pixel values using a {@link Point}.
   *
   * @param mat
   * @param point
   * @return the value of the pixel in {@code mat} at {@code point}.
   */
  public static double[] get(Mat mat, Point point) {
    return mat.get((int) point.y, (int) point.x);
  }

  /**
   * Wrapper method that allows pixel values to be set using a {@link Point}.
   *
   * @param mat
   * @param point
   * @param val the value that the pixel in {@code mat} at {@code point} should have.
   */
  public static void put(Mat mat, Point point, double... val) {
    mat.put((int) point.y, (int) point.x, val);
  }

  /**
   * @param mat a single channel {@link Mat}.
   * @param points
   * @return the mean intensity for all of the {@code points} in {@link Mat}.
   */
  public static double mean(Mat mat, List<Point> points) {
    statArgCheck(mat, points);

    double total = 0;
    for (Point point : points) {
      total += MatUtils.get(mat, point)[0];
    }
    return total / points.size();
  }

  /**
   * @param mat a single channel {@link Mat}.
   * @param points
   * @return the max intensity for all of the {@code points} in {@link Mat}.
   */
  public static double max(Mat mat, List<Point> points) {
    statArgCheck(mat, points);

    double max = 0.0;
    for (Point point : points) {
      double val = MatUtils.get(mat, point)[0];
      if (val > max) {
        max = val;
      }
    }

    return max;
  }

  /**
   * Check's that the parameters are valid for methods that are used to collect simple statistics.
   * 
   * @param mat
   * @param points
   */
  private static void statArgCheck(Mat mat, List<Point> points) {
    if (mat.channels() != 1) {
      throw new IllegalArgumentException("mat must one channel only");
    }
    if (points.isEmpty()) {
      throw new IllegalArgumentException("points cannot be an empty list");
    }
  }

  /**
   * @param mat
   * @param zeroed true if the {@link Mat} returned should have all zero values. Should be false
   *        where possible to improve efficiency.
   * @return a new {@link Mat} with the same dimensions and type as {@code mat}.
   */
  public static Mat similarMat(Mat mat, boolean zeroed) {
    if (zeroed) {
      return Mat.zeros(mat.rows(), mat.cols(), mat.type());
    } else {
      return new Mat(mat.rows(), mat.cols(), mat.type());
    }
  }

  /**
   * @param stack
   * @return List of grey-scale {@link Mat} for the given stack.
   */
  public static List<Mat> getStackMats(CTStack stack) {
    return stack.getSlices().parallelStream().map(MatUtils::getSliceMat)
        .collect(Collectors.toList());
  }

  /**
   * @param slice
   * @return a grey-scale {@link Mat} for the given slice.
   */
  public static Mat getSliceMat(CTSlice slice) {
    // TODO remove all this hacky code when problem fully realised
    int counter = 0;
    while (true) {
      try {
        DICOM dicom = new DICOM();
        dicom.open(IMAGE_DIR + slice.getFilePath());
        return MatUtils.fromDICOM(dicom);
      } catch (Exception e) {
        LOGGER.error("Trying again in one second with slice: " + slice.getId(), e);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          throw new IllegalStateException(e1);
        }
        if (++counter > 3)
          throw e;
      }
    }
  }

}
