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
 * Used to create a csv file where every line is a histogram for a single nodule.
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
  public void writeToFile() throws LungsException {
    LOGGER.info("NoduleHistograms is running...");

    Query<CTSlice> query = filter.all(ds.createQuery(CTSlice.class));
    long numSlice = query.count();
    int counter = 0;

    for (CTSlice slice : query) {
      Mat mat = MatUtils.getSliceMat(slice);

      // Create a histogram for each of the nodules
      Query<GroundTruth> nodules =
          filter.singleReading(ds.createQuery(GroundTruth.class).field("type")
              .equal(GroundTruth.Type.BIG_NODULE));
      for (GroundTruth groundTruth : filter.all(nodules)) {
        writeLine(Histogram.createHist(groundTruth.getRegion(), mat, BINS));
      }

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numSlice + " processed");
      }

    }

    LOGGER.info("NoduleHistograms has finished");
  }

  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new NoduleHistograms().run();
  }

}
