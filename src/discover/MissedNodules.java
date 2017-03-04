package discover;

import static model.GroundTruth.Type.BIG_NODULE;
import static util.MatUtils.getSliceMat;
import static util.MatUtils.grey2BGR;

import java.io.File;
import java.util.ArrayList;
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

import config.Misc;
import model.CTSlice;
import model.GroundTruth;
import util.ColourBGR;
import util.ConfigHelper;
import util.DataFilter;
import util.FutureMonitor;
import util.LungsException;
import util.MongoHelper;
import util.MultiMap;

/**
 * Used collect statics and view examples of nodules that fail to be detected by
 * {@link ml.ROIClassifier}.
 *
 * @author Stuart Clark
 */
public class MissedNodules extends HistogramWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MissedNodules.class);
  private static final String ARG_ERROR =
      "Please provide a single argument either --images or --noimages";
  private static final String IMAGE_SOP_UID = "imageSopUID";
  private static final String IMAGE_DIR = ConfigHelper.getString(Misc.MISSED_NODULES);
  private static final int LOG_INTERVAL = 200;

  private final ExecutorService es;

  /**
   * True if missed nodules should create images showing the nodules that have been missed
   */
  private final boolean images;

  private final Datastore ds;
  private final DataFilter filter;

  public MissedNodules(ExecutorService es, boolean images) throws LungsException {
    super();
    this.es = es;
    this.images = images;
    this.ds = MongoHelper.getDataStore();
    this.filter = DataFilter.get();
  }

  @Override
  protected String fileName() {
    return "missed-nodules-hists.csv";
  }

  @Override
  public void writeToFile() throws LungsException {
    LOGGER.info("MissedNodules running...");

    // Get all the nodules that have been missed
    Query<GroundTruth> query =
        filter.singleReading(filter.all(ds.createQuery(GroundTruth.class).field("type")
            .equal(BIG_NODULE).field("matchedToRoi").equal(false)));
    long numMissed = query.count();
    LOGGER.info(numMissed + " NODULES have been missed");

    // Collect the nodules by imageSopUID
    MultiMap<String, GroundTruth> uidToGt = new MultiMap<>();
    query.cloneQuery().forEach(gt -> uidToGt.putOne(gt.getImageSopUID(), gt));
    LOGGER.info(uidToGt.size() + " slices have missing nodules");

    // Create images if required
    if (images) {
      createImages(uidToGt);
    }

    LOGGER.info("MissedNodules finished running");
  }

  private void createImages(MultiMap<String, GroundTruth> uidToGt) {
    // Clear the image directory
    File dir = new File(IMAGE_DIR);
    dir.delete();
    dir.mkdir();

    // Create a future for each of the slices that needs annotating
    int numSlice = uidToGt.size();
    List<Future> futures = new ArrayList<>(numSlice);
    int counter = 0;
    for (String sopUID : uidToGt.keySet()) {

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
          Imgcodecs.imwrite(IMAGE_DIR + "/" + sopUID + ".jpg", mat);

        }));

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numSlice + " futures created");
      }

    }

    // Monitor futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices processed");
    monitor.monitor();
  }

  public static void main(String[] args) throws LungsException {
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
