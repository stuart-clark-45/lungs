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

import ml.feature.AllHists;
import ml.feature.Area;
import ml.feature.BoundingBox;
import ml.feature.Circularity;
import ml.feature.Convexity;
import ml.feature.Feature;
import ml.feature.FitEllipse;
import ml.feature.HuCircularity;
import ml.feature.LTP;
import ml.feature.MeanIntensity;
import ml.feature.MinCircle;
import ml.feature.Perimeter;
import model.CTSlice;
import model.ROI;
import model.ROIAreaStats;
import util.FutureMonitor;
import util.LungsException;
import util.MatUtils;
import util.MongoHelper;

/**
 * Used to compute {@link Feature}s for {@link ROI}s.
 */
public class FeatureEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEngine.class);
  private static final int LOG_INTERVAL = 1000;
  private static final String IMAGE_SOP_UID = "imageSopUID";

  private List<Feature> primary;
  private List<Feature> secondary;
  private Datastore ds;
  private ROIAreaStats areaStats;

  public FeatureEngine() {
    this(primaryFeatures(), secondaryFeatures());
  }

  /**
   * @param primary a list of primary {@link Feature}s.
   * @param secondary a list of secondary features. These features require information obtained from
   *        the aggregation of primary feature values in order to be computed.
   */
  public FeatureEngine(List<Feature> primary, List<Feature> secondary) {
    this.primary = primary;
    this.secondary = secondary;
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

    // Monitor the progress of computing the primary features
    FutureMonitor monitor = new FutureMonitor(createFutures(es, sopUIDs, primary));
    monitor.setLogString("ROI's primary features have been computed");
    monitor.monitor();

    // Perform required aggregations for secondary features
    ROIAreaStats.compute();

    // Monitor the progress of computing the secondary features
    monitor = new FutureMonitor(createFutures(es, sopUIDs, secondary));
    monitor.setLogString("ROI's secondary features have been computed");
    monitor.monitor();

    LOGGER.info("Finished computing features");
  }

  private List<Future> createFutures(ExecutorService es, List sopUIDs, List<Feature> features) {
    List<Future> futures = new ArrayList<>();
    int counter = 0;
    long numROI = ds.createQuery(ROI.class).count();

    // For each of the SOP UIDs
    LOGGER.info("Creating futures...");
    for (Object obj : sopUIDs) {
      String sopUID = (String) obj;

      // Load the Mat
      CTSlice slice = ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).equal(sopUID).get();
      Mat mat = MatUtils.getSliceMat(slice);

      // Get all the ROIs for the sopUID
      Query<ROI> rois = ds.createQuery(ROI.class).field(IMAGE_SOP_UID).equal(sopUID);

      // Create a future for each of the ROIs and add it to the list
      for (ROI roi : rois) {
        futures.add(es.submit(() -> {

          // Compute all the features for the ROI
            computeFeatures(roi, mat, features);

            // Update the ROI
            ds.save(roi);
          }));

        if (++counter % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + numROI + " futures created");
        }
      }

    }

    return futures;
  }

  /**
   * Compute all {@code features} for the {@code roi}.
   *
   * @param roi
   * @param mat the {@link Mat} where the {@code roi} is found.
   */
  public void computeAllFeatures(ROI roi, Mat mat) {

    // Compute primary features
    for (Feature feature : primary) {
      try {
        feature.compute(roi, mat);
      } catch (LungsException e) {
        LOGGER.error("Failed to compute feature for mat with id: " + roi.getId(), e);
      }
    }

    // Compute secondary features
    for (Feature feature : secondary) {
      try {
        feature.compute(roi, mat);
      } catch (LungsException e) {
        LOGGER.error("Failed to compute feature for mat with id: " + roi.getId(), e);
      }
    }
  }

  /**
   * Compute all {@code features} for the {@code roi}.
   * 
   * @param roi
   * @param mat the {@link Mat} where the {@code roi} is found.
   * @param features
   */
  private void computeFeatures(ROI roi, Mat mat, List<Feature> features) {
    for (Feature feature : features) {
      try {
        feature.compute(roi, mat);
      } catch (LungsException e) {
        LOGGER.error("Failed to compute feature for mat with id: " + roi.getId(), e);
      }
    }
  }

  /**
   * @return list of primary features to use.
   */
  private static List<Feature> primaryFeatures() {
    List<Feature> features = new ArrayList<>();
    features.add(new MeanIntensity());
    features.add(new Area());
    features.add(new Perimeter());
    features.add(new FitEllipse());
    features.add(new BoundingBox());
    features.add(new MinCircle());
    features.add(new Circularity());
    features.add(new Convexity());
    features.add(new HuCircularity());
    return features;
  }

  /**
   * @return list of secondary features to use. These features require information obtained from the
   *         aggregation of primary feature values in order to be computed.
   */
  private static List<Feature> secondaryFeatures() {
    List<Feature> features = new ArrayList<>();
    features.add(new LTP());
    features.add(new AllHists());
    return features;
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new FeatureEngine().run(es);
  }
}
