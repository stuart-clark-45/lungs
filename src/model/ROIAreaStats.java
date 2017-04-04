package model;

import org.mongodb.morphia.Datastore;

import util.MongoHelper;

/**
 * Simple model used to hold statistics about the {@link ROI#area}s in the database. There should
 * only ever be a single instance of this class stored in the database. As such static helper
 * methods have been implemented to access and update the instance stored.
 *
 * @author Stuart Clark
 */
public class ROIAreaStats {

  private static final Datastore DS = MongoHelper.getDataStore();

  private static ROIAreaStats singleton;

  private int min;
  private double mean;
  private int max;

  private ROIAreaStats() {
    // Force the use of static methods.
  }

  public int getMin() {
    return min;
  }

  public double getMean() {
    return mean;
  }

  public int getMax() {
    return max;
  }

  /**
   * @return the {@link ROIAreaStats} that is stored in the database.
   * @throws IllegalStateException if there is no ROIAreaStats in database
   */
  public static ROIAreaStats get() {
    if(singleton == null){
      singleton = DS.createQuery(ROIAreaStats.class).get();
      if (singleton == null) {
        throw new IllegalStateException("No ROIAreaStats have been computed for this database");
      }
    }
    return singleton;
  }


  /**
   * Computes the values for and and updates the {@link ROIAreaStats} stored in the database.
   * 
   * @return the newly computed {@link ROIAreaStats}.
   */
  public static void compute() {
    // Check there are ROIs in database
    long count = DS.createQuery(ROI.class).count();
    if (count == 0) {
      throw new IllegalStateException("There are no ROIs in the database");
    }

    // Compute the statistics
    singleton = new ROIAreaStats();
    singleton.mean = 0;
    singleton.min = Integer.MAX_VALUE;
    singleton.max = Integer.MIN_VALUE;
    for (ROI roi : DS.createQuery(ROI.class)) {
      Integer area = roi.getArea();

      singleton.mean += area;

      if (singleton.min > area) {
        singleton.min = area;
      }

      if (singleton.max < area) {
        singleton.max = area;
      }

    }
    singleton.mean /= count;

    // Update the database
    DS.getCollection(ROIAreaStats.class).drop();
    DS.save(singleton);
  }

  /**
   * Used in tests
   */
  static void clear(){
    singleton = null;
  }

}
