package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.ROI;
import util.FutureMonitor;
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
  }

  /**
   * @param query the {@link Query} used to obtain the {@link CTSlice}s that {@link ROI}s should be
   *        generated for.
   */
  public ROIGenerator(ExecutorService es, Query<CTSlice> query) {
    super(ROI.class);
    this.query = query;
    this.es = es;
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

    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices have had ROIs extracted");
    monitor.monitor();

    LOGGER.info("Finished generating ROIs");
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Datastore ds = MongoHelper.getDataStore();
    Query<CTSlice> query = ds.createQuery(CTSlice.class).field("seriesInstanceUID")
        .equal("1.3.6.1.4.1.14519.5.2.1.6279.6001.137773550852881583165286615668");
    new ROIGenerator(es, query).run();
  }
}
