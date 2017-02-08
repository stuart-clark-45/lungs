package data;

import static core.ROIClassifier.Class.NODULE;
import static core.ROIClassifier.Class.NON_NODULE;
import static util.DataFilter.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import core.Lungs;
import model.CTSlice;
import model.GroundTruth;
import model.ROI;
import util.ConfigHelper;
import util.FutureMonitor;
import util.LungsException;
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

  public ROIGenerator(ExecutorService es) {
    super(ROI.class);
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
    for (CTSlice slice : filter(MongoHelper.getDataStore().createQuery(CTSlice.class))) {
      futures.add(es.submit(() -> {

        // Load slice mat
          Mat mat = Lungs.getSliceMat(slice);

          // Segment slice
          List<Mat> segmented = lungs.segment(Collections.singletonList(mat));

          // Get ground truths for slice
          List<GroundTruth> groundTruths =
              ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                  .field("imageSopUID").equal(slice.getImageSopUID()).asList();

          // Create ROIs and save them
          try {
            List<ROI> rois = lungs.roiExtraction(Collections.singletonList(slice), segmented);
            for (ROI roi : rois) {
              setClass(roi, groundTruths);
              ds.save(roi);
            }

          } catch (LungsException e) {
            LOGGER.error("Failed to extract ROI for stack with SOP UID: " + slice.getImageSopUID(),
                e);
          }
        }));
    }

    // Monitor the progress of the Futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices have had ROIs extracted");
    monitor.monitor();

    LOGGER.info("Finished generating ROIs");
  }

  /**
   * Set the {@link ROI#classificaiton} for {@code roi} by matching the {@link ROI} to a
   * {@link GroundTruth}.
   * 
   * @param roi
   * @param groundTruths
   */
  private void setClass(ROI roi, List<GroundTruth> groundTruths) {
    // Find the highest matching score
    double bestScore = 0.0;
    for (GroundTruth gt : groundTruths) {
      double score = Matcher.match(roi, gt);
      if (score > bestScore) {
        bestScore = score;
      }
    }

    // Set the class
    if (bestScore > MATCH_THRESHOLD) {
      roi.setClassificaiton(NODULE);
    } else {
      roi.setClassificaiton(NON_NODULE);
    }

    // Set the match threshold used
    roi.setMatchThreshold(MATCH_THRESHOLD);
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new ROIGenerator(es).run();
  }

}
