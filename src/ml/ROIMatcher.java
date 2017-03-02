package ml;

import static model.GroundTruth.Type.BIG_NODULE;

import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.GroundTruth;
import model.ROI;
import util.MongoHelper;
import vision.Matcher;

/**
 * Used to match {@link ROI}s to the {@link GroundTruth} that they
 *
 * @author Stuart Clark
 */
public class ROIMatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIMatcher.class);
  private static final String IMAGE_SOP_UID = "imageSopUID";
  private static final int LOG_INTERVAL = 10;

  private Datastore ds;
  private Double matchThreshold;

  public ROIMatcher(double matchThreshold) {
    this();
    this.matchThreshold = matchThreshold;
  }

  public ROIMatcher() {
    this.ds = MongoHelper.getDataStore();
  }

  public void run() {
    LOGGER.info("Running ROIClassifier...");

    clearGtRois();

    LOGGER.info("Beginning matching...");
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
      rois.parallelStream().forEach(roi -> match(roi, gts));

      // Save the updated ROIs
      ds.save(rois);
    }

    LOGGER.info("ROIClassifier finished");
  }

  /**
   * Set all {@link GroundTruth#rois} in database to an empty list.
   */
  public void clearGtRois() {
    LOGGER.info("Setting GroundTruth.rois for empty list for all in database...");
    UpdateOperations<GroundTruth> updateOperation =
        ds.createUpdateOperations(GroundTruth.class).set("rois", new ArrayList<>());
    Query<GroundTruth> query = ds.createQuery(GroundTruth.class);
    ds.update(query, updateOperation);
  }

  /**
   * Set the {@link ROI#classification} for {@code roi} by matching the {@link ROI} to a
   * {@link GroundTruth}.
   *
   * @param roi
   * @param groundTruths
   */
  @SuppressWarnings("ConstantConditions")
  public void match(ROI roi, List<GroundTruth> groundTruths) {
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

    // If there were any matches at all
    if(bestMatch != null){
      // Set the match score found
      roi.setMatchScore(bestScore);

      // Update the ground truth
      bestMatch.setMatchedToRoi(true);
      bestMatch.addRoi(roi);

      // Classify the ROI if matchThreshold has been set
      if (matchThreshold != null) {
        if (bestScore >= matchThreshold) {
          roi.setClassification(ROI.Class.NODULE);
        } else {
          roi.setClassification(ROI.Class.NON_NODULE);
        }
        roi.setMatchThreshold(matchThreshold);
      }
    }

  }

  public static void main(String... args) {
    new ROIMatcher().run();
  }

}
