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

  private final List<List<SigmaMat>> octaves;

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
    SigmaMat base = smooth(doubleSize, 1.0, BASE_SIGMA);

    // Calculate the number of octaves in the pyramid top level should be ~2x2 pixels
    Mat baseMat = base.getMat();
    numOctave = (int) (log(min(baseMat.rows(), baseMat.cols())) / log(2) - 2);

    octaves = new ArrayList<>(numOctave);
    computeGaussians(base);
    gaussianToDog();
  }

  private void computeGaussians(SigmaMat base) {
    double k = pow(2, 1 / S);
    double sigma = BASE_SIGMA;

    for (int i = 0; i < numOctave; i++) {

      // Create a list for the octave
      List<SigmaMat> octave = new ArrayList<>(GAUSSIAN_OCTAVE_SIZE);

      // Add the first image to the octave
      if (i == 0) {
        octave.add(base);
      } else {
        octave.add(subSample(octaves.get(i - 1).get(GAUSSIAN_OCTAVE_SIZE - 3)));
      }

      // Add the other images to the octave
      for (int j = 0; j < GAUSSIAN_OCTAVE_SIZE - 1; j++) {
        double nextSigma = sigma + k;
        octave.add(smooth(octave.get(j), nextSigma));
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
      List<SigmaMat> gaussian = octaves.get(i);
      List<SigmaMat> dogs = new ArrayList<>(DOG_OCTAVE_SIZE);
      SigmaMat first = gaussian.get(0);

      for (int j = 0; j < GAUSSIAN_OCTAVE_SIZE - 1; j++) {
        Mat diff = MatUtils.similarMat(first.getMat(), false);
        SigmaMat a = gaussian.get(j);
        SigmaMat b = gaussian.get(j + 1);
        Core.absdiff(a.getMat(), b.getMat(), diff);
        dogs.add(new SigmaMat(diff, a.getSigma()));
      }

      octaves.set(i, dogs);
    }
  }

  private SigmaMat smooth(SigmaMat source, double desired) {
    return smooth(source.getMat(), source.getSigma(), desired);
  }

  /**
   * @param source the source {@link SigmaMat} for the gaussian blur.
   * @param current the sigma value for {@code mat}.
   * @param desired the desired sigma value for the return {@code Mat}.
   * @return a new {@link SigmaMat} with a sigma value of {@code desired}.
   */
  private SigmaMat smooth(Mat source, double current, double desired) {
    double sigma = sqrt(pow(desired, 2) - pow(current, 2));
    Mat smoothed = MatUtils.similarMat(source, false);
    Imgproc.GaussianBlur(source, smoothed, new Size(SMOOTHING_SIZE, SMOOTHING_SIZE), sigma);
    return new SigmaMat(smoothed, sigma);
  }

  private SigmaMat subSample(SigmaMat sigmaMat) {
    Mat mat = sigmaMat.getMat();
    Mat sampled = new Mat(mat.rows() / 2, mat.cols() / 2, mat.type());
    Imgproc.resize(mat, sampled, sampled.size(), 0, 0, INTER_NEAREST);
    return new SigmaMat(sampled, sigmaMat.getSigma());
  }

  public void display() {
    List<Mat> mats = new ArrayList<>();
    List<String> titles = new ArrayList<>();

    int numOctaves = octaves.size();
    int octaveSize = octaves.get(0).size();

    for (int i = 0; i < octaves.size(); i++) {
      List<SigmaMat> octave = octaves.get(i);
      for (int j = 0; j < octaveSize; j++) {
        SigmaMat sigmaMat = octave.get(j);
        mats.add(sigmaMat.getMat());
        titles.add("Octave " + (i + 1) + "/" + numOctaves + " - Mat " + (j + 1) + "/" + octaveSize
            + " simga=" + sigmaMat.getSigma());
      }
    }

    MatViewer viewer = new MatViewer(mats);
    viewer.setMatTitles(titles);
    viewer.display();
  }

  public List<List<SigmaMat>> getOctaves() {
    return octaves;
  }

}
