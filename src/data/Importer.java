package data;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

  /**
   * The path to use when importing files
   */
  protected final String path;

  protected final Datastore ds;

  /**
   * The class of the model that your will parse your data into.
   */
  private final Class<T> clazz;

  /**
   * @param clazz the class of the model that your will parse your data into.
   */
  public Importer(Class<T> clazz) {
    this.clazz = clazz;
    ds = MongoHelper.getDataStore();
    Mode.Value mode = ConfigHelper.getMode();
    if (mode == Mode.Value.TEST) {
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
      DBCollection collection = ds.getCollection(clazz);
      collection.drop();
      collection.dropIndexes();
      importModels();

      LOGGER.info("Ensuring indexes for " + clazz.getName() + "...");
      ds.ensureIndexes(clazz);
      LOGGER.info("Finished ensuring indexes for " + clazz.getName());
    } catch (LungsException e) {
      throw new IllegalStateException("Failed to import models", e);
    }
  }

  /**
   * @return the path that should be used mode is set to {@link Mode.Value#TEST}. Simply return
   *         {@code null} file is not used to import models.
   */
  protected abstract String testPath();

  /**
   * @return the path that should be used mode is not set to {@link Mode.Value#TEST}. Simply return
   *         {@code null} file is not used to import models.
   */
  protected abstract String normalPath();

  protected abstract void importModels() throws LungsException;

}
