package util;

import java.util.Arrays;

import org.mongodb.morphia.query.Query;

/**
 * Used to provide global access to a filter used so that only a subset of the data is processed by
 * the system
 *
 * @author Stuart Clark
 */
public class DataFilter {

  public static final String TRAIN_INSTANCE =
      "1.3.6.1.4.1.14519.5.2.1.6279.6001.137773550852881583165286615668";

  public static final String TEST_INSTANCE =
      "1.3.6.1.4.1.14519.5.2.1.6279.6001.118140393257625250121502185026";

  private static final String MODEL = "Sensation 16";

  public static <T> Query<T> filter(Query<T> query) {
    return query.field("model").equal(MODEL).field("seriesInstanceUID")
        .in(Arrays.asList(TRAIN_INSTANCE, TEST_INSTANCE));
  }

}
