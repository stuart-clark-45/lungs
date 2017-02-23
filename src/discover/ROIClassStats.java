package discover;

import static model.GroundTruth.Type.BIG_NODULE;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.GroundTruth;
import model.ROI;
import util.DataFilter;
import util.MongoHelper;

/**
 * Used to obtain statics relating to the classes that have been assigned to the {@link ROI}s
 * produced by the {@link ml.MLPipeline}.
 *
 * @author Stuart Clark
 */
public class ROIClassStats {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIClassStats.class);

  public static void main(String[] args) {
    Datastore ds = MongoHelper.getDataStore();
    DataFilter filter = DataFilter.get();

    long numROI = ds.createQuery(ROI.class).count();
    long numNoduleROI =
        ds.createQuery(ROI.class).field("classification").equal(ROI.Class.NODULE).count();
    LOGGER.info(numNoduleROI + "/" + numROI + " ROIs were classified as nodules");

    // For each of the readings
    for (Object obj : ds.getCollection(GroundTruth.class).distinct("readingNumber")) {
      Integer readingNumber = (Integer) obj;

      // Find the number of nodules that should be in the data set according to the reading
      Query<GroundTruth> query =
          ds.createQuery(GroundTruth.class).field("readingNumber").equal(readingNumber)
              .field("type").equal(BIG_NODULE);
      long expectedNodule = filter.all(query).count();
      LOGGER.info("There should be " + expectedNodule + " nodules according to reading "
          + readingNumber);
    }

  }

}
