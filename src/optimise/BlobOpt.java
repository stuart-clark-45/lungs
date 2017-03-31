package optimise;

import static config.Segmentation.SURE_BG_FRAC;
import static config.Segmentation.SURE_FG;
import static config.Segmentation.Filter.KERNEL_SIZE;
import static config.Segmentation.Filter.SIGMA_COLOUR;
import static config.Segmentation.Filter.SIGMA_SPACE;
import static javax.swing.UIManager.getInt;
import static util.ConfigHelper.getDouble;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.SegOpt;
import core.Lungs;
import model.ROI;
import util.ConfigHelper;
import vision.BilateralFilter;
import vision.BlobDetector;
import vision.ROIExtractor;

/**
 * Used to optimise {@link core.Lungs#extractJuxtapleural(Mat, List, Mat)}. Using two consecutive
 * binary searches.
 *
 * @author Stuart Clark
 */
public class BlobOpt {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobOpt.class);

  private final LungsOptHelper helper;

  public BlobOpt(int numStacks) {
    helper = new LungsOptHelper(numStacks);
  }

  public void run() {
    LOGGER.info("Running BlobOpt...");

    // An instance of lungs which will return the maximum number of blobs
    Lungs unfiltered = createLungs(10, 255);
    List<List<ROI>> allROIs = helper.extractROIs(unfiltered);

    double idealInclusion = 0.5 * helper.noduleInclusion(unfiltered);
    LOGGER.info("ideal inclusion is " + idealInclusion);

    /*
     * Perform a binary search to find the optimum dogThresh
     */
    int upperBound = 255;
    int lowerBound = 0;
    int dogThresh = -1;
    int minROI = numROIs(allROIs);
    while (upperBound == lowerBound) {

      LOGGER.info("There are " + minROI + " ROIs");

      // Use mid value as dogThresh
      dogThresh = lowerBound + (upperBound - lowerBound + 1) / 2;
      LOGGER.info("Running with dogThresh of " + dogThresh);
      allROIs = helper.extractROIs(createLungs(dogThresh, Integer.MAX_VALUE));
      double inclusion = helper.noduleInclusion(allROIs);

      // If inclusion has dropped then mid was too high a value
      if (inclusion < idealInclusion) {
        lowerBound = dogThresh;

      } else {

        int numROI = numROIs(allROIs);

        // If the number of ROIs has increased then then mid was too low a value
        if (numROI > minROI) {
          lowerBound = dogThresh;
        } else {
          upperBound = dogThresh;
          minROI = numROI;
        }

      }
    }

    LOGGER.info("Optimum dogThresh is " + dogThresh);

    /*
     * Perform a binary search to find the optimum gradientThresh
     */
    upperBound = 255;
    lowerBound = 0;
    int gradientThresh = -1;
    while (upperBound > lowerBound) {

      LOGGER.info("There are " + minROI + " ROIs");

      // Use mid value as gradientThresh
      gradientThresh = lowerBound + (upperBound - lowerBound + 1) / 2;
      LOGGER.info("Running with gradient thresh of " + gradientThresh);
      allROIs = helper.extractROIs(createLungs(dogThresh, gradientThresh));
      double inclusion = helper.noduleInclusion(allROIs);

      // If inclusion has dropped then mid was too low a value
      if (inclusion < idealInclusion) {
        lowerBound = gradientThresh;

      } else {

        int numROI = numROIs(allROIs);

        // If the number of ROIs has increased then then mid was too high a value
        if (numROI > minROI) {
          upperBound = gradientThresh;
        } else {
          lowerBound = gradientThresh;
          minROI = numROI;
        }

      }
    }

    LOGGER.info("Optimum dogThresh is " + dogThresh + ", optimum gradientThresh is "
        + gradientThresh);

    LOGGER.info("Finished running BlobOpt");
  }

  private int numROIs(List<List<ROI>> allROIs) {
    return allROIs.stream().mapToInt(List::size).sum();
  }

  private Lungs createLungs(int dogThresh, int gradientThresh) {
    // Create the filter
    BilateralFilter filter =
        new BilateralFilter(getInt(KERNEL_SIZE), getInt(SIGMA_COLOUR), getInt(SIGMA_SPACE));

    // Create the ROI extractor
    ROIExtractor extractor = new ROIExtractor(getInt(SURE_FG), getDouble(SURE_BG_FRAC));

    // Create the blob detector
    BlobDetector detector = new BlobDetector(dogThresh, gradientThresh);

    // Segment the Mats
    Lungs lungs = new Lungs(filter, extractor, detector);
    lungs.setSolitary(false);

    return lungs;
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new BlobOpt(ConfigHelper.getInt(SegOpt.STACKS)).run();
  }

}
