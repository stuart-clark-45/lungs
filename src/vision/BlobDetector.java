package vision;

import static config.Segmentation.Blob.DOG_THRESH;
import static config.Segmentation.Blob.GRADIENT_THRESH;
import static config.Segmentation.Blob.NEIGHBOURHOOD_DEPTH;
import static config.Segmentation.Blob.NEIGHBOURHOOD_HEIGHT;
import static config.Segmentation.Blob.NEIGHBOURHOOD_WIDTH;
import static config.Segmentation.Blob.NUM_SIGMA;
import static util.ConfigHelper.getInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.sun.istack.internal.Nullable;

import model.KeyPoint;
import util.MatUtils;

/**
 * Uses a SIFT-like algorithm to detect blobs in a {@link Mat}.
 *
 * @author Stuart Clark
 */
public class BlobDetector {

  private static final double SIGMA_RATIO = 1.6;

  // TODO java doc
  private final List<Double> sigmaValues;
  private final int numSigmaValues;
  private final int xPadding;
  private final int yPadding;
  private final int sigmaDiff;
  private final int dogThresh;
  private final int gradientThresh;

  /**
   * @param neighbourhood the dimensions of the neighbourhood used for checking if a point in sigma
   *        space is a key point. Takes the form {width, height, depth}.
   * @param dogThresh the threshold used when deciding if a point in sigma space could be a key
   *        point (values higher than this can be key points)
   * @param gradientThresh the threshold used when deciding if a key point is an edge (and hence
   *        should be filtered out)
   * @param numSigma the number of different sigma values to use when computing DOG.
   */
  public BlobDetector(int[] neighbourhood, int dogThresh, int gradientThresh, int numSigma) {
    this.xPadding = neighbourhood[0] / 2;
    this.yPadding = neighbourhood[1] / 2;
    this.sigmaDiff = neighbourhood[2] / 2;
    this.sigmaValues = new ArrayList<>();
    // sigmaValues.add(1);
    // for (int i = 3; i < 3 + 6 * (numSigma - 1); i ++) {
    for (double i = 1; i <= numSigma; i++) {
//      double sigma = Math.pow(2, i);
      sigmaValues.add(i);
      // sigmaValues.add(1.6 * sigma);
    }
    this.numSigmaValues = sigmaValues.size();
    this.dogThresh = dogThresh;
    this.gradientThresh = gradientThresh;
  }

  /**
   * @param mat
   * @param validPoints a set of {@link Point}s that could potentially be {@link KeyPoint}s.
   * @return a list of {@link KeyPoint}s that identify the location and size of blobs detected in
   *         {@code mat}.
   */
  public List<KeyPoint> detect(Mat mat, @Nullable Set<Point> validPoints) {
    // Apply gaussian blurs with different sigma values
    List<Mat> blurred = new ArrayList<>(numSigmaValues);
    for (double sigma : sigmaValues) {
      Mat temp = MatUtils.similarMat(mat, false);
      // Sigma used to calculate kernel size
      Imgproc.GaussianBlur(mat, temp, new Size(0, 0), sigma, sigma);
      blurred.add(temp);

      temp = MatUtils.similarMat(mat, false);
      // Sigma used to calculate kernel size
      Imgproc.GaussianBlur(mat, temp, new Size(0, 0), SIGMA_RATIO * sigma, SIGMA_RATIO * sigma);
      blurred.add(temp);
    }

    // Difference of gaussians
    List<Mat> dogs = new ArrayList<>(numSigmaValues - 1);
    for (int i = 0; i < blurred.size() - 1; i += 2) {
      Mat diff = MatUtils.similarMat(mat, false);
      Core.subtract(blurred.get(i), blurred.get(i + 1), diff);
      Mat padded = new Mat();
      Core.copyMakeBorder(diff, padded, yPadding, yPadding, xPadding, xPadding,
          Core.BORDER_REPLICATE);
      dogs.add(padded);
    }

    // Get gradient magnitude for mat
    Mat gradientMag = new Sobel(mat).magnitude();

    // Create key points
    List<KeyPoint> keyPoints = new ArrayList<>();
    for (int dogIndex = 0; dogIndex < dogs.size(); dogIndex++) {
      // If a set of valid points has been provided
      if (validPoints != null) {
        for (Point point : validPoints) {
          createKeyPoint((int) point.y, (int) point.x, dogIndex, dogs, gradientMag).ifPresent(
              keyPoints::add);
        }
        // If no set of valid points has been provided
      } else {
        for (int row = 0; row < mat.rows(); row++) {
          for (int col = 0; col < mat.cols(); col++) {
            createKeyPoint(row, col, dogIndex, dogs, gradientMag).ifPresent(keyPoints::add);
          }
        }
      }
    }

    return keyPoints;
  }

  /**
   * @param row the row for the pixel being examined.
   * @param col the column for the pixel being examined.
   * @param dogIndex the index of {@code dogs} for the DOG that the pixel belongs to.
   * @param dogs a list of DOGs
   * @param gradientMag the gradient magnitudes of the pixels in the original image. Computed using
   *        {@link Sobel#magnitude()}.
   * @return an {@code Optional.of()} the the {@link KeyPoint} if the pixel at {@code row},
   *         {@code col} is a key point in the DOG at {@code dogIndex}. {@link Optional#empty()}
   *         otherwise.
   */
  private Optional<KeyPoint> createKeyPoint(int row, int col, int dogIndex, List<Mat> dogs,
      Mat gradientMag) {

    Mat thisDog = dogs.get(dogIndex);
    double val = thisDog.get(row + yPadding, col + xPadding)[0];
    double gradientVal = gradientMag.get(row, col)[0];

    // Check if point could potentially be valid key points
    if (val < dogThresh || gradientVal > gradientThresh) {
      return Optional.empty();
    }

    // Create a list of the DOGs that should be in the neighbourhood
    List<Mat> matsToCheck = new ArrayList<>();
    for (int i = dogIndex - sigmaDiff; i < dogIndex; i++) {
      if (i >= 0) {
        matsToCheck.add(dogs.get(i));
      }
    }
    for (int i = dogIndex; i <= dogIndex + sigmaDiff; i++) {
      if (i < dogs.size()) {
        matsToCheck.add(dogs.get(i));
      }
    }

    // Iterate over the the neighbourhood to see if points is a maxima, return Optional.empty() if
    // it is not.
    for (int r = row; r <= row + yPadding * 2; r++) {
      for (int c = col; c <= col + xPadding * 2; c++) {
        for (Mat mat : matsToCheck) {
          if (mat.get(r, c)[0] > val) {
            return Optional.empty();
          }
        }
      }
    }

    // Create a KeyPoint for the point and return it
    return Optional.of(new KeyPoint(new Point(col, row), Math.round(sigmaValues.get(dogIndex) * SIGMA_RATIO), val));

  }

  /**
   * @return an instance of {@link BlobDetector} using the parameters taken from
   *         {@code application.conf}.
   */
  public static BlobDetector getInstance() {
    int[] neighbourhood =
        new int[] {getInt(NEIGHBOURHOOD_WIDTH), getInt(NEIGHBOURHOOD_HEIGHT),
            getInt(NEIGHBOURHOOD_DEPTH)};
    return new BlobDetector(neighbourhood, getInt(DOG_THRESH), getInt(GRADIENT_THRESH),
        getInt(NUM_SIGMA));
  }

}
