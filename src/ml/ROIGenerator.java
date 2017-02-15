package ml;

import static util.DataFilter.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import data.Importer;
import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.GroundTruth;
import model.roi.ROI;
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
    for (CTSlice slice : filter(MongoHelper.getDataStore().createQuery(CTSlice.class))) {
      futures.add(es.submit(() -> {

        // Load slice mat
          Mat mat = Lungs.getSliceMat(slice);

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
              ROIClassifier.setClass(roi, groundTruths);
            }

            // Save rois
            ds.save(rois);
          } catch (LungsException e) {
            LOGGER.error("Failed to extract ROI for slice with SOP UID: " + slice.getImageSopUID(),
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

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new ROIGenerator(es).run();
  }

}
