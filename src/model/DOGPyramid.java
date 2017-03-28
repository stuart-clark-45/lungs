package model;

import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import util.MatUtils;
import util.MatViewer;

/**
 * TODO
 *
 * @author Stuart Clark
 */
public class DOGPyramid {

  private static final double SMOOTHING_SIZE = 7;

  /**
   * The number of DOGs TODO finishs this
   */
  private static final int S = 3;
  private static final int GAUSSIAN_OCTAVE_SIZE = S + 3;
  private static final int DOG_OCTAVE_SIZE = S + 2;
  private static final double BASE_SIGMA = 1.6;

  private final int numOctave;

  private final List<List<Mat>> octaves;

  public DOGPyramid(Mat mat) {
    /*
     * We assume that the original image has a blur of at least σ = 0.5 (the minimum needed to
     * prevent significant aliasing), and that therefore the doubled image has σ = 1.0 relative to
     * its new pixel spacing (Lowe 2004).
     */
    Mat doubleSize = new Mat(mat.rows() * 2, mat.cols() * 2, mat.type());
    Imgproc.resize(mat, doubleSize, doubleSize.size(), 0, 0, INTER_LINEAR);

    /*
     * Providing assumptions above are true base has a blur of at least σ = 1.6 as specified in
     * (Lowe 2004).
     */
    Mat base = smooth(doubleSize, 1, BASE_SIGMA);

    // Calculate the number of octaves in the pyramid top level should be ~2x2 pixels
    numOctave = (int) (log(min(base.rows(), base.cols())) / log(2) - 2);

    octaves = new ArrayList<>(numOctave);
    computeGaussians(base);
    gaussianToDog();
  }

  private void computeGaussians(Mat base) {
    double k = pow(2, 1 / S);
    double sigma = BASE_SIGMA;

    for (int i = 0; i < numOctave; i++) {

      // Create a list for the octave
      List<Mat> octave = new ArrayList<>(GAUSSIAN_OCTAVE_SIZE);

      // Add the first image to the octave
      if (i == 0) {
        octave.add(base);
      } else {
        octave.add(subSample(octaves.get(i - 1).get(GAUSSIAN_OCTAVE_SIZE - 1)));
      }

      // Add the other images to the octave
      for (int j = 0; j < GAUSSIAN_OCTAVE_SIZE - 1; j++) {
        double nextSigma = sigma + k;
        octave.add(smooth(octave.get(j), BASE_SIGMA, nextSigma));
        sigma = nextSigma;
      }

      // Add the octave to the octaves
      octaves.add(octave);
    }
  }

  /**
   * Replaces the gaussian octaves with difference of gaussian octaves. Replaced rather than stored
   * separately to conserve memory.
   */
  private void gaussianToDog() {
    for (int i = 0; i < octaves.size(); i++) {
      List<Mat> gaussian = octaves.get(i);
      List<Mat> dogs = new ArrayList<>(DOG_OCTAVE_SIZE);
      Mat first = gaussian.get(0);

      for (int j = 0; j < GAUSSIAN_OCTAVE_SIZE - 1; j++) {
        Mat diff = MatUtils.similarMat(first, false);
        // Due to bug in library following line actually means gaussian.get(j + 1)
        // - gaussian.get(j).
        Core.absdiff(gaussian.get(j), gaussian.get(j + 1), diff);
        dogs.add(diff);
      }

      octaves.set(i, dogs);
    }
  }

  /**
   * @param mat the source {@link Mat} for the gaussian blur.
   * @param current the sigma value for {@code mat}.
   * @param desired the desired sigma value for the return {@code Mat}.
   * @return a new {@link Mat} with a sigma value of {@code desired}.
   */
  private Mat smooth(Mat mat, double current, double desired) {
    double sigma = sqrt(pow(desired, 2) - pow(current, 2));
    Mat smoothed = MatUtils.similarMat(mat, false);
    Imgproc.GaussianBlur(mat, smoothed, new Size(SMOOTHING_SIZE, SMOOTHING_SIZE), sigma);
    return smoothed;
  }

  private Mat subSample(Mat mat) {
    Mat subSampled = new Mat(mat.rows() / 2, mat.cols() / 2, mat.type());
    Imgproc.resize(mat, subSampled, subSampled.size(), 0, 0, INTER_NEAREST);
    return subSampled;
  }

  public void display() {
    List<Mat> mats = new ArrayList<>();
    List<String> titles = new ArrayList<>();

    int numOctaves = octaves.size();
    int octaveSize = octaves.get(0).size();

    for (int i = 0; i < octaves.size(); i++) {
      List<Mat> octave = octaves.get(i);
      for (int j = 0; j < octaveSize; j++) {
        mats.add(octave.get(j));
        titles.add("Octave " + (i + 1) + "/" + numOctaves + " - Mat " + (j + 1) + "/" + octaveSize);
      }
    }

    MatViewer viewer = new MatViewer(mats);
    viewer.setMatTitles(titles);
    viewer.display();
  }

}
