package ml;

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

import core.Lungs;
import data.Importer;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.roi.ROI;
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
  private final Lungs lungs;

  private ExecutorService es;

  public ROIGenerator(ExecutorService es) {
    super(ROI.class);
    this.es = es;
    lungs = new Lungs();
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

          // Load slice mat
            Mat mat = MatUtils.getSliceMat(slice);

            // Segment slice (will only ever be one returned)
            Mat segmented = lungs.segment(Collections.singletonList(mat)).get(0);

            // Get ground truths for slice
            List<GroundTruth> groundTruths =
                ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                    .field("imageSopUID").equal(slice.getImageSopUID()).asList();

            // Create ROIs and save them
            try {
              List<ROI> rois = lungs.extractRois(segmented);

              // Set ROI fields
              for (ROI roi : rois) {
                roi.setImageSopUID(slice.getImageSopUID());
                roi.setSeriesInstanceUID(slice.getSeriesInstanceUID());
                roi.setSet(set);
                ROIClassifier.setClass(roi, groundTruths);
              }

              // Save rois
              ds.save(rois);
            } catch (LungsException e) {
              LOGGER.error(
                  "Failed to extract ROI for slice with SOP UID: " + slice.getImageSopUID(), e);
            }
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
