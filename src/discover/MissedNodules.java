package discover;

import static model.GroundTruth.Type.BIG_NODULE;
import static model.Histogram.POS_VALS_8BIT;
import static util.MatUtils.getSliceMat;
import static util.MatUtils.grey2BGR;
import static util.MatUtils.put;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
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
import model.Histogram;
import util.ColourBGR;
import util.ConfigHelper;
import util.DataFilter;
import util.FutureMonitor;
import util.LungsException;
import util.MatUtils;
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
  private static final String IMAGE_DIR = ConfigHelper.getString(Misc.MISSED_NODULES);
  private static final String DARK_DIR = IMAGE_DIR + "/dark";
  private static final String LIGHT_DIR = IMAGE_DIR + "/light";
  private static final int LOG_INTERVAL = 200;

  /**
   * The threshold used to distinguish between dark and light nodules.
   */
  private static final double DARK_LIGHT_THRESH = ConfigHelper.getInt(Misc.DARK_LIGHT_THRESH);

  private final ExecutorService es;

  /**
   * True if missed nodules should create images showing the nodules that have been missed
   */
  private final boolean images;

  private final Datastore ds;
  private final DataFilter filter;

  /**
   * Used to create a csv containing histogram data for nodules that have a very dark ROIs.
   */
  private final HistogramWriter darkWriter;

  /**
   * Used to create a csv containing histogram data for nodules that have a very light ROIs (these
   * are likely to be juxtapleural nodules that are missed).
   */
  private final HistogramWriter lightWriter;

  /**
   * Used to give each of the images a unique id so that images for CTSlices with the same
   * imageSopUID do not overwrite one another.
   */
  private AtomicInteger id;

  public MissedNodules(ExecutorService es, boolean images) throws LungsException {
    super();
    this.es = es;
    this.images = images;
    this.ds = MongoHelper.getDataStore();
    this.filter = DataFilter.get();

    this.darkWriter = new HistogramWriter() {
      @Override
      protected String fileName() {
        return "dark-nodules.csv";
      }
    };


    this.lightWriter = new HistogramWriter() {
      @Override
      protected String fileName() {
        return "light-nodules.csv";
      }
    };

  }

  public void run() throws LungsException {
    LOGGER.info("MissedNodules running...");

    // Create the imageSopUID to GroundTruth multimap
    MultiMap<String, GroundTruth> uidToGt = buildUidToGt();

    // Reset the image directories
    if (images) {
      resetDirs();
    }

    // Reset counters
    id = new AtomicInteger(0);

    // Create a future for each of the slices that needs annotating
    int numSlice = uidToGt.size();
    List<Future> futures = new ArrayList<>(numSlice);
    int counter = 0;
    for (String sopUID : uidToGt.keySet()) {

      // Submit runnable for slice (use Callable to avoid try catch blocks)
      futures.add(es.submit((Callable<Void>) () -> {
        // Get Mats for the slice
          CTSlice slice = ds.createQuery(CTSlice.class).field(IMAGE_SOP_UID).equal(sopUID).get();
          Mat mat = getSliceMat(slice);

          // Annotate GroundTruths on Mat
          for (GroundTruth gt : uidToGt.get(sopUID)) {

            // Decide if the nodule is a dark or light nodule
            HistogramWriter writer;
            String dir;
            List<Point> region = gt.getRegion();
            if (MatUtils.mean(mat, region) > DARK_LIGHT_THRESH) {
              writer = lightWriter;
              dir = LIGHT_DIR;
            } else {
              writer = darkWriter;
              dir = DARK_DIR;
            }

            // Create the histogram and write to the file
            Histogram histogram = new Histogram(POS_VALS_8BIT, POS_VALS_8BIT);
            histogram.add(region, mat);
            histogram.computeBins();
            histogram.toFrequencies();
            writer.writeLine(histogram);

            // Create an annotated image and save it
            if (images) {
              Mat bgr = grey2BGR(mat);
              for (Point point : gt.getEdgePoints()) {
                put(bgr, point, ColourBGR.RED);
              }
              Imgcodecs.imwrite(dir + "/" + id.getAndIncrement() + "-" + sopUID + ".bmp", bgr);
            }

          }

          return null;
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

    darkWriter.close();
    lightWriter.close();

    LOGGER.info("MissedNodules finished running");
  }

  private MultiMap<String, GroundTruth> buildUidToGt() {
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

    return uidToGt;
  }

  /**
   * Delete the directories that images are stored in (if they exist), then create new ones.
   */
  private void resetDirs() throws LungsException {
    try {
      File imageDir = new File(IMAGE_DIR);
      FileUtils.deleteDirectory(imageDir);
      imageDir.delete();
      imageDir.mkdir();
      new File(DARK_DIR).mkdir();
      new File(LIGHT_DIR).mkdir();
    } catch (IOException e) {
      throw new LungsException("Failed to reset dirs", e);
    }
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
