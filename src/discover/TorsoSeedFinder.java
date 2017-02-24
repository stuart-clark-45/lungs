package discover;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Segmentation;
import model.CTSlice;
import util.ConfigHelper;
import util.DataFilter;
import util.MatUtils;
import util.MongoHelper;
import util.PointUtils;

/**
 * Used to find the best possible seed point for extracting the large region in CTScans that defines
 * the torso shape.
 *
 * @author Stuart Clark
 */
public class TorsoSeedFinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TorsoSeedFinder.class);

  /**
   * The value used for the foreground in the segmented images.
   */
  private static final int FOREGROUND = 255;

  private static final int LOG_INTERVAL = 100;

  /**
   * The threshold used when creating the mask that is applied to slices.
   */
  private static final int MASK_THRESHOLD = ConfigHelper.getInt(Segmentation.MASK_THRESHOLD);

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    Datastore ds = MongoHelper.getDataStore();

    // Get all of the CTSlices used by the system
    Query<CTSlice> query = DataFilter.get().all(ds.createQuery(CTSlice.class));

    // Get the number of rows in the first CT slice
    CTSlice first = query.cloneQuery().get();
    Integer numRows = first.getRows();
    // Use the first ten cols as possible seeds
    int numCols = 300;

    // Create a set of the seed that could potentially be used
    Set<Point> seeds = new HashSet<>();
    for (int row = 0; row < numRows; row++) {
      for (int col = 0; col < numCols; col++) {
        seeds.add(new Point(col, row));
      }
    }

    // For each of the slices in the database
    long numSlice = query.count();
    int counter = 0;
    for (CTSlice slice : query) {
      Mat mat = MatUtils.getSliceMat(slice);

      if (mat.rows() != numRows) {
        LOGGER.error(slice.getId() + " has " + mat.rows() + " expected " + numRows + " rows");
        continue;
      }

      Mat thresholded = MatUtils.similarMat(mat);
      Imgproc.threshold(mat, thresholded, MASK_THRESHOLD, FOREGROUND, THRESH_BINARY);

      // Remove any seeds that are not marked as FOREGROUND in the thresholded image
      for (int row = 0; row < numRows; row++) {
        for (int col = 0; col < numCols; col++) {
          if (mat.get(row, 0)[0] != FOREGROUND) {
            seeds.remove(new Point(col, row));
            if(seeds.isEmpty()){
              LOGGER.info("There were no viable seeds");
              return;
            }
          }
        }
      }

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numSlice + " slices processed");
      }
    }

    // Sort the row indexes
    List<Point> sorted = PointUtils.xySort(new ArrayList<>(seeds));

    LOGGER.info("The possible seed values are " + sorted);
    LOGGER.info("The middle seed position is " + sorted.get(sorted.size() / 2));

  }

}
