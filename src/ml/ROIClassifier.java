package ml;

import static model.GroundTruth.Type.BIG_NODULE;
import static model.ROI.Class.NODULE;
import static model.ROI.Class.NON_NODULE;

import java.util.List;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import model.GroundTruth;
import model.ROI;
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
  private static final int LOG_INTERVAL = 10;

  private Datastore ds;
  private double matchThreshold;

  public ROIClassifier() {
    this(ConfigHelper.getDouble(Misc.MATCH_THRESHOLD));
  }

  public ROIClassifier(double matchThreshold) {
    this.matchThreshold = matchThreshold;
    this.ds = MongoHelper.getDataStore();
  }

  public void run() {
    LOGGER.info("Running ROIClassifier...");

    // For each of the images we have created ROIs for
    List sopUIDs = ds.getCollection(ROI.class).distinct(IMAGE_SOP_UID);
    for (int i = 0; i < sopUIDs.size(); i++) {
      // Logging
      if (i % LOG_INTERVAL == 0) {
        LOGGER.info(i + "/" + sopUIDs.size() + " images processed");
      }

      String sopUID = (String) sopUIDs.get(i);

      // Get the corresponding ROIs
      List<ROI> rois = ds.createQuery(ROI.class).field(IMAGE_SOP_UID).equal(sopUID).asList();

      // Get the corresponding GroundTruths
      List<GroundTruth> gts =
          ds.createQuery(GroundTruth.class).field(IMAGE_SOP_UID).equal(sopUID).field("type")
              .equal(BIG_NODULE).asList();

      // Classify each of the rois
      rois.parallelStream().forEach(roi -> setClass(roi, gts));

      // Save the updated ROIs
      ds.save(rois);
    }

    LOGGER.info("ROIClassifier finished");
  }

  /**
   * Set the {@link ROI#classification} for {@code roi} by matching the {@link ROI} to a
   * {@link GroundTruth}.
   *
   * @param roi
   * @param groundTruths
   */
  @SuppressWarnings("ConstantConditions")
  public void setClass(ROI roi, List<GroundTruth> groundTruths) {
    // Find the highest matching score
    double bestScore = 0.0;
    GroundTruth bestMatch = null;
    for (GroundTruth gt : groundTruths) {
      double score = Matcher.match(roi, gt);
      if (score > bestScore) {
        bestScore = score;
        bestMatch = gt;
      }
    }

    // Set the match threshold used
    roi.setMatchThreshold(matchThreshold);

    // Set the class
    if (bestScore > matchThreshold) {
      roi.setClassification(NODULE);
      bestMatch.setMatchedToRoi(true);
      bestMatch.setRoi(roi);
    } else {
      roi.setClassification(NON_NODULE);
    }

  }

  public static void main(String... args) {
    new ROIClassifier().run();
  }

}
