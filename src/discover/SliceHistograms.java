package discover;

import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.CTSlice;
import model.roi.Histogram;
import util.LungsException;
import util.MatUtils;

/**
 * Used to create a csv file where every line is a histogram for a singe CT slice.
 *
 * @author Stuart Clark
 */
public class SliceHistograms extends HistogramWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SliceHistograms.class);

  public SliceHistograms() throws LungsException {
    super();
  }

  @Override
  protected String fileName() {
    return "slice-histograms.csv";
  }

  @Override
  public void writeToFile() throws LungsException {
    LOGGER.info("SliceHistograms is running...");

    Query<CTSlice> query = filter.all(ds.createQuery(CTSlice.class));
    long numSlice = query.count();
    int counter = 0;

    for (CTSlice slice : query) {
      // Create the histogram and write a a line in the csv file
      Mat mat = MatUtils.getSliceMat(slice);
      writeLine(Histogram.createHist(mat, BINS));

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numSlice + " processed");
      }

    }

    LOGGER.info("SliceHistograms has finished");
  }

  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new SliceHistograms().run();
  }

}
