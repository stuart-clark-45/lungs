package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Mode;
import model.CTStack;

/**
 * Used to provide global access to a singleton {@link DataFilter} used to obtain useful subsets of
 * the data set.
 *
 * @author Stuart Clark
 */
public class DataFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataFilter.class);

  /**
   * Field name used for queries
   */
  private static final String UID = "seriesInstanceUID";

  /**
   * The model of CT scanner that the system is being trained to use
   */
  static final String MODEL = "Sensation 16";

  /**
   * The proportion of instances that should be used for training
   */
  static final double TRAIN_WEIGHT = 0.8;

  /**
   * The seriesInstanceUID of the instanced used for training while in {@link Mode.Value#TEST} or
   * {@link Mode.Value#DEV} mode.
   */
  public static final String TRAIN_INSTANCE =
      "1.3.6.1.4.1.14519.5.2.1.6279.6001.137773550852881583165286615668";

  /**
   * The seriesInstanceUID of the instanced used for testing while in {@link Mode.Value#TEST} or
   * {@link Mode.Value#DEV} mode.
   */
  public static final String TEST_INSTANCE =
      "1.3.6.1.4.1.14519.5.2.1.6279.6001.118140393257625250121502185026";

  /**
   * The singleton instance
   */
  private static DataFilter DATA_FILTER = null;

  /**
   * All the seriesInstanceUIDs of the instanced used for training while in {@link Mode.Value#PROD}
   * mode.
   */
  List<String> prodTrainInstances;

  /**
   * All the seriesInstanceUIDs of the instanced used for testing while in {@link Mode.Value#PROD}
   * mode.
   */
  List<String> prodTestInstances;

  private Mode.Value mode;

  /**
   * This constructor should not be used (other than testing) use {@link DataFilter#get()} instead.
   */
  DataFilter() {
    mode = ConfigHelper.getMode();
    if (mode == Mode.Value.PROD) {
      initProd();
    }
  }

  private void initProd() {
    LOGGER.info("Separating test and training instances for production...");

    // Create a list of all the distinct seriesInstanceUID that will be used in prod mode
    List<String> uids = new LinkedList<>();
    Datastore ds = MongoHelper.getDataStore();
    Query<CTStack> match = all(ds.createQuery(CTStack.class));
    ds.createAggregation(CTStack.class).match(match).group(UID).aggregate(Result.class)
        .forEachRemaining(result -> uids.add(result.seriesInstanceUID));

    // Calculate the number that should be in the test and training sets
    int total = uids.size();
    int numTrain = Double.valueOf(total * TRAIN_WEIGHT).intValue();
    int numTest = total - numTrain;

    // Create list of training instances
    prodTrainInstances = new ArrayList<>(numTrain);
    for (int i = 0; i < numTrain; i++) {
      prodTrainInstances.add(uids.get(i));
    }

    // Create list of testing instances
    prodTestInstances = new ArrayList<>(numTest);
    for (int i = numTrain; i < total; i++) {
      prodTestInstances.add(uids.get(i));
    }

    LOGGER.info("Finished separating instances");
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns both the test and training set instances.
   */
  public <T> Query<T> all(Query<T> query) {
    if (mode == Mode.Value.PROD) {
      return query.field("model").equal(MODEL);
    } else {
      return query.field(UID).in(Arrays.asList(TRAIN_INSTANCE, TEST_INSTANCE));
    }
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns training set instances only.
   */
  public <T> Query<T> train(Query<T> query) {
    if (mode == Mode.Value.PROD) {
      return all(query).field(UID).in(prodTrainInstances);
    } else {
      return query.field(UID).equal(TRAIN_INSTANCE);
    }
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns testing set instances only.
   */
  public <T> Query<T> test(Query<T> query) {
    if (mode == Mode.Value.PROD) {
      return all(query).field(UID).in(prodTestInstances);
    } else {
      return query.field(UID).equal(TEST_INSTANCE);
    }
  }

  /**
   * @return the singleton {@link DataFilter}.
   */
  public static DataFilter get() {
    if (DATA_FILTER == null) {
      DATA_FILTER = new DataFilter();
    }

    return DATA_FILTER;
  }

  /**
   * Used as the return class for the aggregation that takes place in {@link DataFilter#initProd()}
   */
  private static class Result {
    @Id
    private String seriesInstanceUID;
  }

}
