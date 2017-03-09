package util;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.display.SourceImage;

import ij.plugin.DICOM;
import model.CTSlice;
import model.CTStack;

/**
 * Suite of utility methods that can be used to process {@link Mat}s.
 *
 * @author Stuart Clark
 */
public class MatUtils {

  private static final int BIT_DEPTH = 16;

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
    int cvType = mat.type();
    if (cvType == CvType.CV_16SC1 || cvType == CvType.CV_16SC3) {

      int channels = mat.channels();
      int cols = mat.cols();
      int rows = mat.rows();

      // Create the colour space
      int CS_ID = channels == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_LINEAR_RGB;
      ColorSpace colourSpace = ColorSpace.getInstance(CS_ID);
      int[] bits = new int[channels];
      Arrays.fill(bits, BIT_DEPTH);
      ComponentColorModel colourModel =
          new ComponentColorModel(colourSpace, bits, false, false, Transparency.OPAQUE,
              DataBuffer.TYPE_SHORT);

      // Create the data buffer
      DataBuffer buffer = new DataBufferShort(cols * rows * channels, channels);

      // Create the sample model
      int[] offsets = channels == 1 ? new int[1] : new int[] {0, 1, 2};
      SampleModel sampleModel =
          new ComponentSampleModel(DataBuffer.TYPE_SHORT, cols, rows, channels, cols * channels,
              offsets);

      // Create the raster
      WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
      for (int row = 0; row < mat.rows(); row++) {
        for (int col = 0; col < mat.cols(); col++) {
          raster.setPixel(col, row, mat.get(row, col));
        }
      }

      // Create and return the buffered image
      return new BufferedImage(colourModel, raster, colourModel.isAlphaPremultiplied(),
          new Hashtable<>());

    } else {
      throw new IllegalStateException("Mat with type " + CvType.typeToString(mat.type())
          + " is not supported");
    }
  }

  /**
   * @param dicom
   * @return a {@link Mat} read from {@code dicom}.
   */
  public static Mat fromDICOM(DICOM dicom) {
    BufferedImage bi = dicom.getBufferedImage();
    Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_16SC3);
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
    Mat bgr = new Mat(mat.rows(), mat.cols(), CvType.CV_16SC3);

    for (int i = 0; i < mat.rows(); i++) {
      for (int j = 0; j < mat.cols(); j++) {
        double val = mat.get(i, j)[0];
        bgr.put(i, j, val, val, val);
      }
    }

    return bgr;
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

  /**
   * @param stack
   * @return List of grey-scale {@link Mat} for the given stack.
   */
  public static List<Mat> getStackMats(CTStack stack) throws LungsException {
    List<Mat> mats = new ArrayList<>();
    for (CTSlice slice : stack.getSlices()) {
      mats.add(getSliceMat(slice));
    }
    return mats;
  }

  /**
   * @param slice
   * @return a grey-scale {@link Mat} for the given slice.
   */
  public static Mat getSliceMat(CTSlice slice) throws LungsException {
    try {
      // Load the slice image
      BufferedImage bi = new SourceImage(slice.getFilePath()).getBufferedImage();
      Raster data = bi.getData();

      // Convert to an OpenCV Mat
      Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_16SC1);
      for (int row = 0; row < mat.rows(); row++) {
        for (int col = 0; col < mat.cols(); col++) {
          int[] val = data.getPixel(col, row, new int[1]);
          mat.put(row, col, val[0]);
        }
      }

      // Return the Mat
      return mat;
    } catch (IOException | DicomException e) {
      throw new LungsException("Failed to load CTSlice with id: " + slice.getId());
    }
  }

}
