package vision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.KeyPoint;
import util.MatUtils;

/**
 * Created by stuart on 20/03/2017.
 */
public class BlobDetector {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobDetector.class);

  private final List<Integer> sigmaValues;
  private final int numSigmaValues;
  private final int xPadding;
  private final int yPadding;
  private final int sigmaDiff;

  public BlobDetector() {
    this(new int[] {5, 5, 3});
  }

  /**
   * @param neighbourhood The dimensions of the neighbourhood used for checking if a point in sigma
   *        space is a key point. Takes the form {width, height, sigmaSpace}
   */
  public BlobDetector(int[] neighbourhood) {
    xPadding = neighbourhood[1] / 2;
    yPadding = neighbourhood[2] / 2;
    sigmaValues = Arrays.asList(1, 3, 9, 15, 21, 27);
    numSigmaValues = sigmaValues.size();
    sigmaDiff = numSigmaValues / 2;
  }

  public List<KeyPoint> detect(Mat mat) {
    List<Mat> blurred = new ArrayList<>(numSigmaValues);
    for (Integer sigma : sigmaValues) {
      Mat temp = MatUtils.similarMat(mat);
      Imgproc.GaussianBlur(mat, temp, new Size(0, 0), sigma, sigma);
      blurred.add(temp);
    }

    // Difference of gaussians
    List<Mat> dogs = new ArrayList<>(numSigmaValues - 1);
    for (int j = 0; j < blurred.size() - 1; j++) {
      Mat diff = MatUtils.similarMat(mat);
      Core.subtract(blurred.get(j), blurred.get(j + 1), diff);
      Mat padded = new Mat();
      Core.copyMakeBorder(diff, padded, yPadding, yPadding, xPadding, xPadding,
          Core.BORDER_REPLICATE);
      dogs.add(padded);
    }

    List<KeyPoint> keyPoints = new ArrayList<>();
    for (int dogIndex = 0; dogIndex < dogs.size(); dogIndex++) {
      for (int row = 0; row < mat.rows(); row++) {
        for (int col = 0; col < mat.cols(); col++) {
          createKeyPoint(row, col, dogIndex, dogs).ifPresent(keyPoints::add);
        }
      }
    }

    return keyPoints;
  }

  private Optional<KeyPoint> createKeyPoint(int row, int col, int dogIndex, List<Mat> dogs) {
    Mat thisDog = dogs.get(dogIndex);
     double thisVal = thisDog.get(row + yPadding, col + xPadding)[0];
    if (thisVal < 50) {
      return Optional.empty();
    }

    List<Mat> matsToCheck = new ArrayList<>();
    if (dogIndex - 1 > 1) {
      matsToCheck.add(dogs.get(dogIndex - 1));
    }
    matsToCheck.add(thisDog);
    if (dogIndex + 1 < dogs.size()) {
      matsToCheck.add(dogs.get(dogIndex + 1));
    }

    double min = Double.MAX_VALUE;
    double max = -1;

    // TODO might be worth breaking out of this loop if know not min or max
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

    if (thisVal == min || thisVal == max) {
      return Optional.of(new KeyPoint(new Point(col, row), sigmaValues.get(dogIndex), thisVal));
    } else {
      return Optional.empty();
    }

  }

}
