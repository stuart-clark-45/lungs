package ml;

import static model.GroundTruth.Type.BIG_NODULE;
import static model.roi.ROI.Class.NODULE;
import static model.roi.ROI.Class.NON_NODULE;

import java.util.List;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import model.GroundTruth;
import model.roi.ROI;
import util.ConfigHelper;
import util.MongoHelper;
import vision.Matcher;

/**
 * Used {@link GroundTruth} to decide which ROIs belong which {@link ROI.Class}.
 *
 * @author Stuart Clark
 */
public class ROIClassifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIClassifier.class);
  private static final String IMAGE_SOP_UID = "imageSopUID";
  private static final double MATCH_THRESHOLD = ConfigHelper.getDouble(Misc.MATCH_THRESHOLD);
  private static final int LOG_INTERVAL = 10;

  private Datastore ds;

  public ROIClassifier() {
    this.ds = MongoHelper.getDataStore();
  }

  public void run() {
    LOGGER.info("Running ROIClassifier...");

    // For each of the images we have created ROIs for
    List sopUIDs = ds.getCollection(ROI.class).distinct(IMAGE_SOP_UID);
    for (int i = 0; i < sopUIDs.size(); i++) {
      String sopUID = (String) sopUIDs.get(i);

      // Get the corresponding ROIs
      List<ROI> rois = ds.createQuery(ROI.class).field(IMAGE_SOP_UID).equal(sopUID).asList();

      // Get the corresponding GroundTruths
      List<GroundTruth> gts =
          ds.createQuery(GroundTruth.class).field(IMAGE_SOP_UID).equal(sopUID).field("type")
              .equal(BIG_NODULE).asList();

      // Classify each of the rois
      rois.parallelStream().forEach(roi -> setClass(roi, gts, MATCH_THRESHOLD));

      // Save the updated ROIs
      ds.save(rois);

      // Logging
      if (i % LOG_INTERVAL == 0) {
        LOGGER.info(i + "/" + sopUIDs.size() + " images processed");
      }
    }

    LOGGER.info("ROIClassifier finished");
  }

  public static void setClass(ROI roi, List<GroundTruth> groundTruths) {
    setClass(roi, groundTruths, MATCH_THRESHOLD);
  }

  /**
   * Set the {@link ROI#classification} for {@code roi} by matching the {@link ROI} to a
   * {@link GroundTruth}.
   *
   * @param roi
   * @param groundTruths
   * @param matchThreshold
   */
  public static void setClass(ROI roi, List<GroundTruth> groundTruths, double matchThreshold) {
    // Find the highest matching score
    double bestScore = 0.0;
    for (GroundTruth gt : groundTruths) {
      double score = Matcher.match(roi, gt);
      if (score > bestScore) {
        bestScore = score;
      }
    }

    // Set the class
    if (bestScore > matchThreshold) {
      roi.setClassification(NODULE);
    } else {
      roi.setClassification(NON_NODULE);
    }

    // Set the match threshold used
    roi.setMatchThreshold(matchThreshold);
  }

  public static void main(String... args) {
    new ROIClassifier().run();
  }

}
