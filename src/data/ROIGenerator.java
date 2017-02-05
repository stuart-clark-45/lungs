package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.ROI;
import util.LungsException;
import util.MongoHelper;

/**
 * Used to import {@link ROI}s detected
 *
 * @author Stuart Clark
 */
public class ROIGenerator extends Importer<ROI> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIGenerator.class);

  /**
   * The {@link Query} used to obtain the {@link CTSlice}s that {@link ROI}s should be generated
   * for.
   */
  private Query<CTSlice> query;

  private ExecutorService es;

  public ROIGenerator(ExecutorService es) {
    this(es, MongoHelper.getDataStore().createQuery(CTSlice.class).field("model")
        .equal("Sensation 16"));
    this.es = es;
  }

  /**
   * @param query the {@link Query} used to obtain the {@link CTSlice}s that {@link ROI}s should be
   *        generated for.
   */
  public ROIGenerator(ExecutorService es, Query<CTSlice> query) {
    super(ROI.class);
    this.query = query;
  }

  @Override
  protected String testPath() {
    return null;
  }

  @Override
  protected String prodPath() {
    return null;
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {
    LOGGER.info("Generating ROIs this may take some time...");

    Lungs lungs = new Lungs();

    // Submit a runnable for slice that is used to extract the ROIs
    List<Future> futures = new ArrayList<>();
    for (CTSlice slice : query) {
      futures
          .add(es.submit(() -> {
            Mat mat = Lungs.getSliceMat(slice);
            List<Mat> segmented = lungs.segment(Collections.singletonList(mat));
            try {
              lungs.roiExtraction(Collections.singletonList(slice), segmented);
            } catch (LungsException e) {
              LOGGER.error(
                  "Failed to extract ROI for stack with SOP UID: " + slice.getImageSopUID(), e);
            }
          }));
    }

    // Create and start a monitor thread
    new Thread(() -> {
      int counter = 0;
      while (counter < futures.size()) {

        // Count the number of futures that are complete
        counter = 0;
        for (Future future : futures) {
          if (future.isDone()) {
            counter++;
          }
        }

        // Logging
        LOGGER.info(counter + "/" + futures.size() + " slices have had ROIs extracted");

        // Sleep for 6s
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          LOGGER.error("Logging thread interrupted", e);
        }
      }
    }).start();

    // Wait for all the futures to complete
    for (Future future : futures) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error("Failed to get Future", e);
      }
    }

    LOGGER.info("Finished generating ROIs");
  }
}
