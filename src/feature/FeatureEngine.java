package feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.ROI;
import util.FutureMonitor;
import util.MongoHelper;

/**
 * Used to compute {@link Feature}s for {@link ROI}s.
 */
public class FeatureEngine implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEngine.class);
  private static final int LOG_INTERVAL = 1000;
  private static final String IMAGE_SOP_UID = "imageSopUID";

  private ExecutorService es;
  private List<Feature> features;
  private Query<ROI> query;
  private Datastore ds;

  public FeatureEngine(ExecutorService es) {
    this(es, defaultFeatures(), MongoHelper.getDataStore().createQuery(ROI.class));
  }

  public FeatureEngine(ExecutorService es, List<Feature> features, Query<ROI> query) {
    this.es = es;
    this.features = features;
    this.query = query;
    this.ds = MongoHelper.getDataStore();
  }

  @Override
  public void run() {
    LOGGER.info("Computing Features this may take some time...");

    // Find all the distinct SOP UIDs for the ROIs we want to compute feature values for
    Iterator<Result> sopUIDs =
        ds.createAggregation(ROI.class).match(query).group(IMAGE_SOP_UID).aggregate(Result.class);

    List<Future> futures = new ArrayList<>();
    int counter = 0;
    long numROI = query.count();

    // For each of the SOP UIDs
    LOGGER.info("Creating futures...");
    while (sopUIDs.hasNext()) {
      String sopUID = sopUIDs.next().id;

      // Load the Mat
      CTSlice slice = ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).equal(sopUID).get();
      Mat mat = Lungs.getSliceMat(slice);

      // Get all the ROIs for the sopUID
      Query<ROI> rois = ds.createQuery(ROI.class).field(IMAGE_SOP_UID).equal(sopUID);

      // Create a future for each of the ROIs and add it to the list
      for (ROI roi : rois) {
        futures.add(es.submit(() -> {

          // Compute all the features for the ROI
            for (Feature feature : features) {
              feature.compute(roi, mat);
            }

            // Update the ROI
            ds.save(roi);
          }));

        if (++counter % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + numROI + " futures created");
        }
      }

    }

    // Monitor the progress of the Futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("ROI's features computed");
    monitor.monitor();

    LOGGER.info("Finished computing features");
  }

  /**
   * @return list of default features to use.
   */
  private static List<Feature> defaultFeatures() {
    List<Feature> features = new ArrayList<>();
    features.add(new MeanIntensity());
    return features;
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new FeatureEngine(es).run();
  }

  /**
   * Used to obtain the results of the aggregation completed above.
   */
  private static class Result {

    @Id
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

}
