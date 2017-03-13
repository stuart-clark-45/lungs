package discover;

import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.CTSlice;
import model.GroundTruth;
import model.Histogram;
import util.LungsException;
import util.MatUtils;

/**
 * Used to create a csv file with a single line representing a histogram for all the CTSlices that
 * contain nodules.
 *
 * @author Stuart Clark
 */
public class NoduleHistograms extends HistogramWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoduleHistograms.class);

  public NoduleHistograms() throws LungsException {
    super();
  }

  @Override
  protected String fileName() {
    return "nodule-histograms.csv";
  }

  @Override
  public void writeToFile() {
    LOGGER.info("NoduleHistograms is running...");

    // Get all the slices
    Query<CTSlice> query = ds.createQuery(CTSlice.class);
    long numSlice = query.count();

    // Create a histogram for all of the nodules
    double[] combinedBins = new double[Histogram.NUM_POSSIBLE_VALS];

    // Used to count the total number of nodules
    int numNodules = 0;

    // Process each slice
    int sliceCounter = 0;
    int failureCounter = 0;
    for (CTSlice slice : query) {

      try {
        Mat mat = MatUtils.getSliceMat(slice);

        Query<GroundTruth> nodules =
            filter.singleReading(ds.createQuery(GroundTruth.class).field("type")
                .equal(GroundTruth.Type.BIG_NODULE));
        for (GroundTruth groundTruth : nodules) {
          numNodules++;

          // Get the histogram for the nodule
          Histogram noduleHist =
              Histogram.createHist(groundTruth.getRegion(), mat, Histogram.NUM_POSSIBLE_VALS);
          double[] noduleBins = noduleHist.getBins();

          // Add it to the combined histogram
          for (int i = 0; i < noduleBins.length; i++) {
            combinedBins[i] += noduleBins[i];
          }
        }

      } catch (LungsException e) {
        // Some slices fail due to missing images in the data set
        failureCounter++;
        LOGGER.error("Failed to process slice with id: ", slice.getId(), e);
      }

      // Logging
      if (++sliceCounter % LOG_INTERVAL == 0) {
        LOGGER.info(sliceCounter + "/" + numSlice + " processed");
      }

    }

    // Convert the values in the bins to frequencies
    for (int i = 0; i < combinedBins.length; i++) {
      combinedBins[i] /= numNodules;
    }

    // Write the combined histogram to a file
    writeLine(new Histogram(combinedBins));

    LOGGER.info("NoduleHistograms has finished " + (numSlice - failureCounter) + "/" + numSlice
        + " successfully processed");
  }

  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new NoduleHistograms().run();
  }

}
