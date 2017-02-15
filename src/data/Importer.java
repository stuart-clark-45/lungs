package data;

import org.mongodb.morphia.Datastore;

import com.mongodb.DBCollection;

import config.Mode;
import util.ConfigHelper;
import util.LungsException;
import util.MongoHelper;

/**
 * Should be extended by classes that are used to import data into MongoDB.
 *
 * @author Stuart Clark
 */
public abstract class Importer<T> implements Runnable {

  /**
   * The path to use when importing files
   */
  protected final String path;

  /**
   * The class of the model that your will parse your data into.
   */
  private Class<T> clazz;

  /**
   * @param clazz the class of the model that your will parse your data into.
   */
  public Importer(Class<T> clazz) {
    this.clazz = clazz;
    Mode.VALUE mode = ConfigHelper.getMode();
    if (mode == Mode.VALUE.TEST) {
      path = testPath();
    } else {
      path = normalPath();
    }

  }

  /**
   * Drops the current collection and re-imports the data. Indexes are dropped before insertion for
   * efficiency.
   */
  @Override
  public void run() {
    try {
      Datastore ds = MongoHelper.getDataStore();
      DBCollection collection = ds.getCollection(clazz);

      collection.drop();
      collection.dropIndexes();
      importModels(ds);

      ds.ensureIndexes(clazz);
    } catch (LungsException e) {
      throw new IllegalStateException("Failed to import models", e);
    }
  }

  /**
   * @return the path that should be used mode is set to {@link config.Mode.VALUE#TEST}. Simply
   *         return {@code null} file is not used to import models.
   */
  protected abstract String testPath();

  /**
   * @return the path that should be used mode is not set to {@link config.Mode.VALUE#TEST}. Simply
   *         return {@code null} file is not used to import models.
   */
  protected abstract String normalPath();

  protected abstract void importModels(Datastore ds) throws LungsException;

}
