package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   * All the seriesInstanceUIDs of the instanced used for training by the system.
   */
  List<String> trainInstances;

  /**
   * All the seriesInstanceUIDs of the instanced used by the system.
   */
  List<String> testInstances;

  /**
   * All the seriesInstanceUIDs of the instanced used by the system.
   */
  List<String> allInstances;

  private Mode.Value mode;

  /**
   * This constructor should not be used (other than testing) use {@link DataFilter#get()} instead.
   */
  DataFilter() {
    mode = ConfigHelper.getMode();
    if (mode == Mode.Value.PROD) {
      initProd();
    } else {
      trainInstances = Collections.singletonList(TRAIN_INSTANCE);
      testInstances = Collections.singletonList(TEST_INSTANCE);
      allInstances = Stream.of(TRAIN_INSTANCE, TEST_INSTANCE).collect(Collectors.toList());
    }
  }

  private void initProd() {
    LOGGER.info("Separating test and training instances for production...");

    // Create a list of all the distinct seriesInstanceUID that will be used in prod mode
    allInstances = new ArrayList<>();
    Datastore ds = MongoHelper.getDataStore();
    Query<CTStack> match = ds.createQuery(CTStack.class).field("model").equal(MODEL);
    ds.createAggregation(CTStack.class).match(match).group(UID).aggregate(Result.class)
        .forEachRemaining(result -> allInstances.add(result.seriesInstanceUID));

    // Calculate the number that should be in the test and training sets
    int total = allInstances.size();
    int numTrain = Double.valueOf(total * TRAIN_WEIGHT).intValue();
    int numTest = total - numTrain;

    // Create list of training instances
    trainInstances = new ArrayList<>(numTrain);
    for (int i = 0; i < numTrain; i++) {
      trainInstances.add(allInstances.get(i));
    }

    // Create list of testing instances
    testInstances = new ArrayList<>(numTest);
    for (int i = numTrain; i < total; i++) {
      testInstances.add(allInstances.get(i));
    }

    LOGGER.info("Finished separating instances");
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns both the test and training set instances.
   */
  public <T> Query<T> all(Query<T> query) {
    return query.field(UID).in(allInstances);
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns training set instances only.
   */
  public <T> Query<T> train(Query<T> query) {
    return all(query).field(UID).in(trainInstances);
  }

  /**
   * @param query
   * @param <T>
   * @return a query that returns testing set instances only.
   */
  public <T> Query<T> test(Query<T> query) {
    return all(query).field(UID).in(testInstances);
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
