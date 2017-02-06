package feature;

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
import model.CTSlice;
import model.ROI;
import util.FutureMonitor;
import util.MongoHelper;

/**
 * Used to compute {@link Feature}s for {@link ROI}s.
 */
public class FeatureEngine implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEngine.class);

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

    // TODO process image by image not ROI by ROI that way image only needs to be loaded oncefor
    // many ROIs

    List<Future> futures = new ArrayList<>();
    for (ROI roi : query) {
      futures.add(es.submit(() -> {
        // Get the slice for the feature
          CTSlice slice =
              ds.createQuery(CTSlice.class).field("imageSopUID").equal(roi.getImageSopUID()).get();
          Mat mat = Lungs.getSliceMat(slice);

          // Compute all the features for the ROI
          for (Feature feature : features) {
            feature.compute(roi, mat);
          }

          // Update the ROI
          ds.save(roi);
        }));
    }


    // Monitor the progress of the Futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("ROIs features computed");
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

}
