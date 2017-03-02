package ml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import config.Misc;
import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Used to import {@link ROI}s detected
 *
 * @author Stuart Clark
 */
public class ROIGenerator extends Importer<ROI> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIGenerator.class);

  private ExecutorService es;
  private final Lungs lungs;
  private final ROIClassifier classifier;

  public ROIGenerator(ExecutorService es) {
    super(ROI.class);
    this.es = es;
    this.lungs = new Lungs();
    this.classifier = new ROIClassifier(ConfigHelper.getDouble(Misc.MATCH_THRESHOLD));
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

    classifier.clearGtRois();

    // Submit a runnable for slice that is used to extract the ROIs
    List<Future> futures = new ArrayList<>();
    for (CTStack stack : DataFilter.get()
        .all(MongoHelper.getDataStore().createQuery(CTStack.class))) {

      // Determine the set that the stack belongs too
      ROI.Set set;
      if (DataFilter.get().getTrainInstances().contains(stack.getSeriesInstanceUID())) {
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
              classifier.match(roi, groundTruths);
            }

            // Save rois
            ds.save(rois);
          }));
      }
    }

    // Monitor the progress of the Futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices have had ROIs extracted");
    monitor.monitor();

    LOGGER.info("Finished generating ROIs");
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new ROIGenerator(es).run();
  }

}
