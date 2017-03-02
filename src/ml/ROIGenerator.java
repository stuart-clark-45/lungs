package ml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import core.Lungs;
import data.Importer;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.ConfigHelper;
import util.DataFilter;
import util.FutureMonitor;
import util.LungsException;
import util.MatUtils;
import util.MongoHelper;
import vision.Matcher;

/**
 * Used to import {@link ROI}s detected
 *
 * @author Stuart Clark
 */
public class ROIGenerator extends Importer<ROI> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIGenerator.class);
  private static final double MATCH_THRESHOLD = ConfigHelper.getDouble(Misc.MATCH_THRESHOLD);

  private ExecutorService es;
  private final Lungs lungs;
  private final Datastore ds;
  private final DataFilter filter;
  private final ROIClassifier classifier;

  public ROIGenerator(ExecutorService es) {
    super(ROI.class);
    this.es = es;
    this.lungs = new Lungs();
    this.ds = MongoHelper.getDataStore();
    this.filter = DataFilter.get();
    this.classifier = new ROIClassifier(MATCH_THRESHOLD);
  }

  @Override
  protected String testPath() {
    return null;
  }

  @Override
  protected String normalPath() {
    return null;
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {
    LOGGER.info("Generating ROIs this may take some time...");

    clearGtRois();

    // Submit a runnable for slice that is used to extract the ROIs
    List<Future> futures = new ArrayList<>();
    for (CTStack stack : filter.all(MongoHelper.getDataStore().createQuery(CTStack.class))) {

      // Determine the set that the stack belongs too
      ROI.Set set;
      if (filter.getTrainInstances().contains(stack.getSeriesInstanceUID())) {
        set = ROI.Set.TRAIN;
      } else {
        set = ROI.Set.TEST;
      }

      for (CTSlice slice : stack.getSlices()) {
        futures.add(es.submit(() -> {

          // Get ground truths for slice
            List<GroundTruth> groundTruths =
                ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                    .field("imageSopUID").equal(slice.getImageSopUID()).asList();

            // Create ROIs and save them
            List<ROI> rois = lungs.extractRois(MatUtils.getSliceMat(slice));

            // Set ROI fields
            for (ROI roi : rois) {
              roi.setImageSopUID(slice.getImageSopUID());
              roi.setSeriesInstanceUID(slice.getSeriesInstanceUID());
              roi.setSet(set);
              match(roi, groundTruths);
            }

            // Save updated rois and ground truths
            ds.save(rois);
            ds.save(groundTruths);
          }));
      }
    }

    // Monitor the progress of the Futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices have had ROIs extracted");
    monitor.monitor();

    LOGGER.info("Finished generating ROIs");
  }

  /**
   * Set all {@link GroundTruth#rois} in database to an empty list.
   */
  private void clearGtRois() {
    LOGGER.info("Setting GroundTruth.rois for empty list for all in database...");
    UpdateOperations<GroundTruth> updateOperation =
        ds.createUpdateOperations(GroundTruth.class).set("rois", new ArrayList<>());
    Query<GroundTruth> query = ds.createQuery(GroundTruth.class);
    ds.update(query, updateOperation);
  }

  /**
   * Set the {@link ROI#classification} for {@code roi} by matching the {@link ROI} to a
   * {@link GroundTruth}.
   *
   * @param roi
   * @param groundTruths
   */
  @SuppressWarnings("ConstantConditions")
  public void match(ROI roi, List<GroundTruth> groundTruths) {
    // Find the highest matching score
    double bestScore = 0.0;
    GroundTruth bestMatch = null;
    for (GroundTruth gt : groundTruths) {
      double score = Matcher.match(roi, gt);
      if (score > bestScore) {
        bestScore = score;
        bestMatch = gt;
      }
    }

    // If there were any matches at all
    if (bestMatch != null) {
      // Update the roi
      roi.setMatchScore(bestScore);
      classifier.classify(roi);

      // Update the ground truth
      bestMatch.setMatchedToRoi(true);
      bestMatch.addRoi(roi);
    }

  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new ROIGenerator(es).run();
  }

}
