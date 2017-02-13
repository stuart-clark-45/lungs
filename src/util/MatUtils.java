package util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import ij.plugin.DICOM;

/**
 * Suite of utility methods that can be used to process {@link Mat}s.
 *
 * @author Stuart Clark
 */
public class MatUtils {

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
   * @return mat converted to an RGB mat
   */
  public static Mat grey2RGB(Mat mat) {
    Mat rgb = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC3);

    for (int i = 0; i < mat.rows(); i++) {
      for (int j = 0; j < mat.cols(); j++) {
        double val = mat.get(i, j)[0];
        rgb.put(i, j, val, val, val);
      }
    }

    return rgb;
  }

  /**
   * @param grey a single channel {@code Mat}.
   * @param point
   * @return the intensity of the pixel in {@code grey} at {@code point}.
   */
  public static double getIntensity(Mat grey, Point point) {
    return grey.get((int) point.x, (int) point.y)[0];
  }

  /**
   * @param mat
   * @return a new {@link Mat} with the same dimentions and type as {@code mat}.
   */
  public static Mat similarMat(Mat mat) {
    return Mat.zeros(mat.rows(), mat.cols(), mat.type());
  }

}
