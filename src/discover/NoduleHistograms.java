package discover;

import static model.Histogram.POS_VALS_8BIT;

import java.util.List;

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

    // Used to count the total number of nodules
    int numNodules = 0;

    // Process each slice
    int sliceCounter = 0;
    int failureCounter = 0;
    for (CTSlice slice : query) {

      // Get all the nodules
      List<GroundTruth> nodules =
          filter.singleReading(
              ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE))
              .asList();

      // Skip this slice if there are no nodules in it
      if (!nodules.isEmpty()) {
        try {
          Mat mat = MatUtils.getSliceMat(slice);

          for (GroundTruth groundTruth : nodules) {
            numNodules++;

            // Create the histogram and write to the file
            Histogram histogram = new Histogram(POS_VALS_8BIT);
            histogram.createHist(groundTruth.getRegion(), mat, POS_VALS_8BIT);
            writeLine(histogram);
          }

        } catch (LungsException e) {
          // Some slices fail due to missing images in the data set
          LOGGER.error("Failed to process slice with id: ", slice.getId(), e);
        }
      }

      // Logging
      if (++sliceCounter % LOG_INTERVAL == 0) {
        LOGGER.info(sliceCounter + "/" + numSlice + " processed");
      }

    }

    LOGGER.info("NoduleHistograms has finished " + (numSlice - failureCounter) + "/" + numSlice
        + " successfully processed");
  }


  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new NoduleHistograms().run();
  }

}
