package ml;

import java.util.ArrayList;
import java.util.List;
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
import ml.feature.Feature;
import ml.feature.MeanIntensity;
import model.CTSlice;
import model.ROI;
import util.FutureMonitor;
import util.LungsException;
import util.MongoHelper;

/**
 * Used to compute {@link Feature}s for {@link ROI}s.
 */
public class FeatureEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEngine.class);
  private static final int LOG_INTERVAL = 1000;
  private static final String IMAGE_SOP_UID = "imageSopUID";

  private List<Feature> features;
  private Datastore ds;

  public FeatureEngine() {
    this(defaultFeatures());
  }

  public FeatureEngine(List<Feature> features) {
    this.features = features;
    this.ds = MongoHelper.getDataStore();
  }

  /**
   * Compute features for each of the {@link ROI}s in the database.
   * 
   * @param es
   */
  public void run(ExecutorService es) {
    LOGGER.info("Computing Features this may take some time...");

    // Find all the distinct SOP UIDs for the ROIs
    List sopUIDs = ds.getCollection(ROI.class).distinct(IMAGE_SOP_UID);

    List<Future> futures = new ArrayList<>();
    int counter = 0;
    long numROI = ds.createQuery(ROI.class).count();

    // For each of the SOP UIDs
    LOGGER.info("Creating futures...");
    for (Object obj : sopUIDs) {
      String sopUID = (String) obj;

      // Load the Mat
      CTSlice slice = ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).equal(sopUID).get();
      Mat mat = Lungs.getSliceMat(slice);

      // Get all the ROIs for the sopUID
      Query<ROI> rois = ds.createQuery(ROI.class).field(IMAGE_SOP_UID).equal(sopUID);

      // Create a future for each of the ROIs and add it to the list
      for (ROI roi : rois) {
        futures.add(es.submit(() -> {

          // Compute all the features for the ROI
            computeFeatures(roi, mat);

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
   * Compute all features for the {@code roi}.
   * 
   * @param roi
   * @param mat the {@link Mat} where the {@code roi} is found.
   */
  public void computeFeatures(ROI roi, Mat mat) {
    for (Feature feature : features) {
      try {
        feature.compute(roi, mat);
      } catch (LungsException e) {
        LOGGER.error("Failed to compute feature for mat with id: " + roi.getId(), e);
      }
    }
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
    new FeatureEngine().run(es);
  }
}
