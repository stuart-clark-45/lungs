package util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

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

}
