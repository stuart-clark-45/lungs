package util;

import org.mongodb.morphia.query.Query;

/**
 * Used to provide global access to a filter used so that only a subset of the data is processed by
 * the system
 *
 * @author Stuart Clark
 */
public class DataFilter {

  private static final String SERIES_INSTANCE_UID =
      "1.3.6.1.4.1.14519.5.2.1.6279.6001.137773550852881583165286615668";

  private static final String MODEL = "Sensation 16";

  public static <T> Query<T> filter(Query<T> query) {
    return query.field("model").equal(MODEL).field("seriesInstanceUID").equal(SERIES_INSTANCE_UID);
  }

}
