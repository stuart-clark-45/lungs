package model;

import org.mongodb.morphia.annotations.Id;

/**
 * A simple model used to obtain a string as a results of an aggregation using
 * {@link org.mongodb.morphia.aggregation.AggregationPipeline#aggregate(Class)}
 *
 * @author Stuart Clark
 */
public class StringResult {

  @Id
  private String id;

  public String getId() {
    return id;
  }

}
