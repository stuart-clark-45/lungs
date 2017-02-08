package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.GroundTruth;
import util.MongoHelper;
import vision.Matcher;

/**
 * Used to combine {@link GroundTruth}s given by different radiologists into a single reading
 *
 * @author Stuart Clark
 */
public class GroundTruthCombiner implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroundTruthCombiner.class);
  private static final String IMAGE_SOP_UID = "imageSopUID";

  private Datastore ds;

  public GroundTruthCombiner() {
    this.ds = MongoHelper.getDataStore();
  }

  @Override
  public void run() {
    List sopUIDs = ds.getCollection(GroundTruth.class).distinct(IMAGE_SOP_UID);
    for (Object obj : sopUIDs) {
      String sopUID = (String) obj;

      // Get all the ground truths for the same image
      List<GroundTruth> gts =
          ds.createQuery(GroundTruth.class).field(IMAGE_SOP_UID).equal(sopUID).field("type")
              .equal(GroundTruth.Type.BIG_NODULE).asList();

      List<Double> scores = new ArrayList<>();

      // A set of sets of ground truths that are deemed to be for the same nodule
      Set<Set<GroundTruth>> masterSet = new HashSet<>();
      for (int i = 0; i < gts.size(); i++) {
        GroundTruth groundTruth = gts.get(i);

        for (int j = 0; j < gts.size(); j++) {
          // Don't want to compare with self
          if (j == i) {
            continue;
          }
          GroundTruth truth = gts.get(j);

          double score = Matcher.match(groundTruth, truth);
          if(score != 0){
            scores.add(score);
          }

        }

      }

      Collections.sort(scores);
      for (Double s : scores) {
        LOGGER.info(""+s);
      }

//      break;
    }


  }

  public static void main(String[] args) {
    new GroundTruthCombiner().run();
  }
}
