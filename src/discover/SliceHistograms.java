package discover;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.CTSlice;
import model.GroundTruth;
import model.Histogram;
import model.StringResult;
import util.LungsException;
import util.MatUtils;

/**
 * Used to create a csv file where every line is a histogram for a singe CT slice.
 *
 * @author Stuart Clark
 */
public class SliceHistograms extends HistogramWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SliceHistograms.class);
  private static final String IMAGE_SOP_UID = "imageSopUID";

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

    // Find the sopUIDs of all of the slices that have nodules in them
    LOGGER.info("Aggregating GroundTruth to find slices with nodules...");
    Query<GroundTruth> match =
        ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE);
    Iterator<StringResult> results =
        ds.createAggregation(GroundTruth.class).match(match).group(IMAGE_SOP_UID)
            .aggregate(StringResult.class);
    List<String> sopUIDs = new ArrayList<>();
    results.forEachRemaining(r -> sopUIDs.add(r.getId()));

    // Find all slices that have nodules in them
    LOGGER.info("Retrieving slices with nodules...");
    Query<CTSlice> query =
        filter.all(ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).in(sopUIDs));
    long numSlice = query.count();
    int counter = 0;

    // For each slice create the histogram and write it as a line in the csv file
    for (CTSlice slice : query) {
      Mat mat = MatUtils.getSliceMat(slice);
      writeLine(Histogram.createHist(mat, Histogram.POS_VALS_8BIT));

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
