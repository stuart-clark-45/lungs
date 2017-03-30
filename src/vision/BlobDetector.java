package vision;

import static config.Segmentation.Blob.DOG_THRESH;
import static config.Segmentation.Blob.GRADIENT_THRESH;
import static util.ConfigHelper.getInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.sun.istack.internal.Nullable;

import model.DOGPyramid;
import model.KeyPoint;
import model.SigmaMat;
import util.MatUtils;

/**
 * Uses a SIFT-like algorithm to detect blobs in a {@link Mat}.
 *
 * @author Stuart Clark
 */
public class BlobDetector {

  /**
   * The threshold used to set the minimum value (inclusive) that a key point in scale space can
   * have.
   */
  private final int dogThresh;

  /**
   * The threshold used to set the maximum gradient magnitude (inclusive) that a key point can have.
   * A key points gradient magnitude is obtained using the
   */
  private final int gradientThresh;

  private int blobsDetected;

  /**
   * @param dogThresh the threshold used when deciding if a point in sigma space could be a key
   *        point (values higher than this can be key points)
   * @param gradientThresh the threshold used when deciding if a key point is an edge (and hence
   *        should be filtered out)
   */
  public BlobDetector(int dogThresh, int gradientThresh) {
    this.dogThresh = dogThresh;
    this.gradientThresh = gradientThresh;
  }

  /**
   * @param mat
   * @param mask a set of {@link Point}s that could potentially be {@link KeyPoint}s.
   * @return a list of {@link KeyPoint}s that identify the location and size of blobs detected in
   *         {@code mat}.
   */
  public List<KeyPoint> detect(Mat mat, @Nullable Set<Point> mask) {
    // Create DOG pyramid
    DOGPyramid pyramid = new DOGPyramid(mat);

    // Get gradient magnitude for mat
    Mat gradientMag = new Sobel(mat).magnitude();

    // Create list of key points
    List<KeyPoint> keyPoints = new ArrayList<>();
    List<List<SigmaMat>> octaves = pyramid.getOctaves();
    for (List<SigmaMat> octave : octaves) {
      for (int i = 0; i < octave.size() - 2; i++) {
        Mat dogMat = octave.get(i).getMat();
        for (int row = 1; row < dogMat.rows() - 1; row++) {
          for (int col = 1; col < dogMat.cols() - 1; col++) {
            createKeyPoint(row, col, i, octave, gradientMag, mask).ifPresent(keyPoints::add);
          }
        }
      }
    }

    blobsDetected += keyPoints.size();

    return keyPoints;
  }

  /**
   * @param row the row for the pixel being examined.
   * @param col the column for the pixel being examined.
   * @param dogIndex the index of {@code octave} for the DOG that the pixel belongs to.
   * @param octave the list of {@link SigmaMat}s for the octave being examined.
   * @param gradientMag the gradient magnitudes of the pixels in the original image. Computed using
   *        {@link Sobel#magnitude()}.
   * @param mask a set of {@link Point}s that could potentially be {@link KeyPoint}s.
   * @return an {@code Optional.of()} the the {@link KeyPoint} if the pixel at {@code row},
   *         {@code col} is a key point in the DOG at {@code dog}. {@link Optional#empty()}
   *         otherwise.
   */
  private Optional<KeyPoint> createKeyPoint(int row, int col, int dogIndex, List<SigmaMat> octave,
      Mat gradientMag, Set<Point> mask) {

    SigmaMat sigmaMat = octave.get(dogIndex);
    Mat dogMat = sigmaMat.getMat();

    // Get the dog value for the point in scale space
    double dogVal = dogMat.get(row, col)[0];

    // Get the point using the original mats co-ordinate system
    Point scaledPoint = sigmaMat.getScaledPoint(row, col);

    // Get the gradient magnitude for relative pixel of the original mat
    double gradientVal = MatUtils.get(gradientMag, scaledPoint)[0];

    // Check if the scaled point is in the mask (if there is one)
    boolean inMask = mask == null || mask.contains(scaledPoint);

    // Check if point could potentially be a valid key point
    if (!inMask || dogVal < dogThresh || gradientVal > gradientThresh) {
      return Optional.empty();
    }

    // Iterate over the the neighbourhood to see if points is a maxima, return Optional.empty() if
    // it is not.
    for (int r = row - 1; r <= row + 1; r++) {
      for (int c = col - 1; c <= col + 1; c++) {
        for (int dog = dogIndex; dog < dogIndex + 3; dog++) {
          if (octave.get(dog).getMat().get(r, c)[0] > dogVal) {
            return Optional.empty();
          }
        }
      }
    }

    // Create a KeyPoint for the point and return it
    return Optional.of(new KeyPoint(scaledPoint, sigmaMat.getSigma(), dogVal));
  }

  public int getBlobsDetected() {
    return blobsDetected;
  }

  /**
   * @return an instance of {@link BlobDetector} using the parameters taken from
   *         {@code application.conf}.
   */
  public static BlobDetector getInstance() {
    return new BlobDetector(getInt(DOG_THRESH), getInt(GRADIENT_THRESH));
  }
}
