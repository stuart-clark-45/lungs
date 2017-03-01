package optimise;

import static org.mongodb.morphia.aggregation.Group.grouping;

import java.util.Iterator;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ml.ROIClassifier;
import model.GroundTruth;
import model.ROI;
import util.DataFilter;
import util.MongoHelper;

/**
 * Used to optimise the match threshold used by {@link ROIClassifier}.
 *
 * @author Stuart Clark
 */
public class ROIClassifierOptimiser {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIClassifierOptimiser.class);

  private Datastore ds;
  private DataFilter filter;

  public ROIClassifierOptimiser() {
    this.ds = MongoHelper.getDataStore();
    this.filter = DataFilter.get();
  }


  /**
   * Perform a binary search to find the match threshold that should be used to classify {@link ROI}
   * s.
   */
  public void run() {
    int expected = getExpectedNodules();

    double lowerBound = 0.0;
    double upperBound = 1.0;

    double matchThreshold = 0;
    while (upperBound - lowerBound > 0.00001) {
      matchThreshold = lowerBound + (upperBound - lowerBound) / 2;
      LOGGER.info("Running ROIClassifier with match threshold of " + matchThreshold);

      new ROIClassifier(matchThreshold).run();

      long numNoduleROIs =
          ds.createQuery(ROI.class).field("classification").equal(ROI.Class.NODULE).count();

      LOGGER.info(numNoduleROIs + " ROIs were classified as nodules the desired number is "
          + expected);

      if (numNoduleROIs > expected) {
        lowerBound = matchThreshold;
      } else if (numNoduleROIs < expected) {
        upperBound = matchThreshold;
      } else {
        break;
      }
    }

    LOGGER.info("Final match threshold was " + matchThreshold);
  }

  /**
   * @return the maximum number of nodules identified by all the {@link GroundTruth}s of the same
   *         {@link GroundTruth#readingNumber}
   *
   */
  @SuppressWarnings("ConstantConditions")
  private int getExpectedNodules() {
    Query<GroundTruth> match =
        filter.all(ds.createQuery(GroundTruth.class).field("type")
            .equal(GroundTruth.Type.BIG_NODULE));

    // Aggregate the GroundTruths counting the number of nodules for each reading number
    Iterator<NoduleCount> noduleCounts =
        ds.createAggregation(GroundTruth.class).match(match)
            .group(grouping("_id", "readingNumber"), grouping("count", new Accumulator("$sum", 1)))
            .aggregate(NoduleCount.class);

    // Find the the greatest number of nodules and return it
    int highestCount = -1;
    while (noduleCounts.hasNext()) {
      NoduleCount count = noduleCounts.next();
      if (count.count > highestCount) {
        highestCount = count.count;
      }
    }
    return highestCount;
  }

  /**
   * Used as the result for the aggregation made in
   * {@link ROIClassifierOptimiser#getExpectedNodules()}.
   */
  private static class NoduleCount {
    @Id
    private int readingNumber;
    private int count;
  }

  public static void main(String[] args) {
    new ROIClassifierOptimiser().run();
  }

}
