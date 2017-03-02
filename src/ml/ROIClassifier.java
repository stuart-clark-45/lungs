package ml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import model.ROI;
import util.ConfigHelper;
import util.FutureMonitor;
import util.MongoHelper;

/**
 * Used to classify {@link ROI}s.
 *
 * @author Stuart Clark
 */
public class ROIClassifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIClassifier.class);

  private final double matchThreshold;

  public ROIClassifier(double matchThreshold) {
    this.matchThreshold = matchThreshold;
  }

  /**
   * Classifies all the ROIs in the database.
   */
  public void run() {
    LOGGER.info("Running ROIClassifier...");

    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Datastore ds = MongoHelper.getDataStore();

    // Create futures that classify ROIs
    Query<ROI> query = ds.createQuery(ROI.class).field("matchThreshold").notEqual(null);
    List<Future> futures = new ArrayList<>((int) query.count());
    for (ROI roi : query) {
      futures.add(es.submit(() -> {
        classify(roi);
        ds.save(roi);
      }));
    }

    // Monitor futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("ROIs classified");
    monitor.monitor();

    LOGGER.info("Finished running ROIClassifier");
  }

  /**
   * Classify {@code roi} by setting {@link ROI#classification} and {@link ROI#matchThreshold}.
   * 
   * @param roi
   */
  public void classify(ROI roi) {
    Double matchScore = roi.getMatchScore();
    if (matchScore != null) {
      if (matchScore >= matchThreshold) {
        roi.setClassification(ROI.Class.NODULE);
      } else {
        roi.setClassification(ROI.Class.NON_NODULE);
      }
      roi.setMatchThreshold(matchThreshold);
    }
  }

  public static void main(String[] args) {
    new ROIClassifier(ConfigHelper.getDouble(Misc.MATCH_THRESHOLD)).run();
  }
}
