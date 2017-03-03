package discover;

import static model.GroundTruth.Type.BIG_NODULE;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static util.MatUtils.getSliceMat;
import static util.MatUtils.grey2BGR;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.CTSlice;
import model.GroundTruth;
import model.StringResult;
import util.ColourBGR;
import util.DataFilter;
import util.FutureMonitor;
import util.MongoHelper;
import util.MultiMap;

/**
 * Used collect statics and view examples of nodules that fail to be detected by
 * {@link ml.ROIClassifier}.
 *
 * @author Stuart Clark
 */
public class MissedNodules {

  private static final Logger LOGGER = LoggerFactory.getLogger(MissedNodules.class);
  private static final String ARG_ERROR =
      "Please provide a single argument either --images or --noimages";
  private static final String IMAGE_SOP_UID = "imageSopUID";
  private static final String IMAGE_DIR = "./missed-nodules/";
  private static final int LOG_INTERVAL = 200;

  private final ExecutorService es;

  /**
   * True if missed nodules should create images showing the nodules that have been missed
   */
  private final boolean images;

  private final Datastore ds;
  private final DataFilter filter;

  public MissedNodules(ExecutorService es, boolean images) {
    this.es = es;
    this.images = images;
    this.ds = MongoHelper.getDataStore();
    this.filter = DataFilter.get();
  }

  public void run() {
    LOGGER.info("MissedNodules running...");

    // Get all the nodules that have been missed
    Query<GroundTruth> query =
        filter.singleReading(filter.all(ds.createQuery(GroundTruth.class).field("type")
            .equal(BIG_NODULE).field("matchedToRoi").equal(false)));
    long numMissed = query.count();
    LOGGER.info(numMissed + " NODULES have been missed");

    MultiMap<String, GroundTruth> uidToGt = new MultiMap<>();
    query.cloneQuery().forEach(gt -> uidToGt.putOne(gt.getImageSopUID(), gt));

    // Get a list of all the imageSopUIDs that will be needed
    Iterator<StringResult> results =
        ds.createAggregation(GroundTruth.class).match(query).group(grouping("_id", IMAGE_SOP_UID))
            .aggregate(StringResult.class);
    List<String> sopUIDs = new ArrayList<>();
    results.forEachRemaining(result -> sopUIDs.add(result.getId()));
    int numSlice = sopUIDs.size();
    LOGGER.info(numSlice + " slices have missing nodules");

    // Create images if required
    if (images) {

      // Clear the image directory
      File dir = new File(IMAGE_DIR);
      dir.delete();
      dir.mkdir();

      // Create a future for each of the slices that needs annotating
      List<Future> futures = new ArrayList<>(numSlice);
      for (int i = 0; i < numSlice; i++) {

        // Logging
        if (i % LOG_INTERVAL == 0) {
          LOGGER.info(i + "/" + numSlice + " futures created");
        }

        String sopUID = sopUIDs.get(i);

        // Submit runnable for slice
        futures.add(es.submit(() -> {
          // Get a BGR Mat for the slice
            CTSlice slice = ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).equal(sopUID).get();
            Mat mat = grey2BGR(getSliceMat(slice));

            // Annotate GroundTruths on Mat
            for (GroundTruth gt : uidToGt.get(sopUID)) {
              for (Point point : gt.getEdgePoints()) {
                mat.put((int) point.y, (int) point.x, ColourBGR.RED);
              }
            }

            // Save the mat
            Imgcodecs.imwrite(IMAGE_DIR + sopUID + ".jpg", mat);

          }));

      }

      // Monitor futures
      FutureMonitor monitor = new FutureMonitor(futures);
      monitor.setLogString("slices processed");
      monitor.monitor();

      LOGGER.info("MissedNodules finished running");
    }

  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Check there is one arg
    if (args.length != 1) {
      throw new IllegalArgumentException(ARG_ERROR);
    }

    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Parse the arg and run MissedNodules
    String arg = args[0];
    if (arg.equalsIgnoreCase("--images")) {
      new MissedNodules(es, true).run();
    } else if (arg.equalsIgnoreCase("--noimages")) {
      new MissedNodules(es, false).run();
    } else {
      throw new IllegalArgumentException(ARG_ERROR);
    }
  }
}
