package vision;

import static org.opencv.core.CvType.CV_16S;
import static org.opencv.core.CvType.CV_32FC1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import model.KeyPoint;
import util.MatUtils;

/**
 * Uses a SIFT-like algorithm to detect blobs in a {@link Mat}.
 *
 * @author Stuart Clark
 */
public class BlobDetector {

  private final List<Integer> sigmaValues;
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
    sigmaValues.add(1);
    for (int i = 3; i < 3 + 6 * (numSigma - 1); i += 6) {
      sigmaValues.add(i);
    }
    this.numSigmaValues = sigmaValues.size();
    this.dogThresh = dogThresh;
    this.gradientThresh = gradientThresh;
  }

  /**
   * @param mat
   * @return a list of {@link KeyPoint}s that identify the location and size of blobs detected in
   *         {@code mat}.
   */
  public List<KeyPoint> detect(Mat mat) {
    // Apply gaussian blurs with different sigma values
    List<Mat> blurred = new ArrayList<>(numSigmaValues);
    for (Integer sigma : sigmaValues) {
      Mat temp = MatUtils.similarMat(mat, false);
      Imgproc.GaussianBlur(mat, temp, new Size(0, 0), sigma, sigma);
      blurred.add(temp);
    }

    // Difference of gaussians
    List<Mat> dogs = new ArrayList<>(numSigmaValues - 1);
    for (int j = 0; j < blurred.size() - 1; j++) {
      Mat diff = MatUtils.similarMat(mat, false);
      Core.subtract(blurred.get(j), blurred.get(j + 1), diff);
      Mat padded = new Mat();
      Core.copyMakeBorder(diff, padded, yPadding, yPadding, xPadding, xPadding,
          Core.BORDER_REPLICATE);
      dogs.add(padded);
    }

    // Get gradient magnitude for mat
    Mat gradientMag = gradientMagnitude(mat);

    // Create key points
    List<KeyPoint> keyPoints = new ArrayList<>();
    for (int dogIndex = 0; dogIndex < dogs.size(); dogIndex++) {
      for (int row = 0; row < mat.rows(); row++) {
        for (int col = 0; col < mat.cols(); col++) {
          createKeyPoint(row, col, dogIndex, dogs, gradientMag).ifPresent(keyPoints::add);
        }
      }
    }

    return keyPoints;
  }

  /**
   * @param mat
   * @return a {@link Mat} containing the gradient magnitude for each pixel in {@code mat}.
   */
  private Mat gradientMagnitude(Mat mat) {
    // Gradient X
    Mat gradX = new Mat(mat.rows(), mat.cols(), CV_16S);
    Imgproc.Sobel(mat, gradX, CV_16S, 1, 0);

    // Gradient Y
    Mat gradY = new Mat(mat.rows(), mat.cols(), CV_16S);
    Imgproc.Sobel(mat, gradY, CV_16S, 0, 1);

    // Gradient X squared
    Mat gradXPow2 = new Mat(mat.rows(), mat.cols(), CV_16S);
    Core.pow(gradX, 2, gradXPow2);

    // Gradient Y squared
    Mat gradYPow2 = new Mat(mat.rows(), mat.cols(), CV_16S);
    Core.pow(gradY, 2, gradYPow2);

    // Calculate gradient magnitude
    Mat sum = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    Core.add(gradXPow2, gradYPow2, sum);
    Mat sumFloat = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    sum.convertTo(sumFloat, sumFloat.type());
    Mat gradientMag = new Mat(mat.rows(), mat.cols(), CV_32FC1);
    Core.sqrt(sumFloat, gradientMag);

    return gradientMag;
  }

  /**
   * @param row the row for the pixel being examined.
   * @param col the column for the pixel being examined.
   * @param dogIndex the index of {@code dogs} for the DOG that the pixel belongs to.
   * @param dogs a list of DOGs
   * @param gradientMag the gradient magnitudes of the pixels in the original image. Computed using
   *        {@link BlobDetector#gradientMagnitude(Mat)}
   * @return an {@code Optional.of()} the the {@link KeyPoint} if the pixel at {@code row},
   *         {@code col} is a key point in the DOG at {@code dogIndex}. {@link Optional#empty()}
   *         otherwise.
   */
  private Optional<KeyPoint> createKeyPoint(int row, int col, int dogIndex, List<Mat> dogs,
      Mat gradientMag) {

    Mat thisDog = dogs.get(dogIndex);
    double thisVal = thisDog.get(row + yPadding, col + xPadding)[0];

    // Check if point could potentially be valid key points
    if (thisVal < dogThresh || gradientMag.get(row, col)[0] < gradientThresh) {
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

    // Iterate over the the neighbourhood
    // TODO might be worth breaking out of this loop if know not min or max
    double min = Double.MAX_VALUE;
    double max = -1;
    for (int r = row; r <= row + yPadding * 2; r++) {
      for (int c = col; c <= col + xPadding * 2; c++) {
        for (Mat mat : matsToCheck) {
          double thatVal = mat.get(r, c)[0];
          if (thatVal < min) {
            min = thatVal;
          }
          if (thatVal > max) {
            max = thatVal;
          }
        }
      }
    }

    // Check if the point is an extrema and create a KeyPoint if it is
    if (thisVal == min || thisVal == max) {
      return Optional.of(new KeyPoint(new Point(col, row), sigmaValues.get(dogIndex), thisVal));
    } else {
      return Optional.empty();
    }

  }

}
