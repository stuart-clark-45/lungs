package data;

import org.mongodb.morphia.Datastore;

import com.mongodb.DBCollection;

import util.LungsException;
import util.MongoHelper;

/**
 * Should be extended by classes that are used to import data into MongoDB.
 *
 * @author Stuart Clark
 */
public abstract class Importer<T> implements Runnable {

  /**
   * The class of the model that your will parse your data into.
   */
  private Class<T> clazz;

  /**
   * @param clazz the class of the model that your will parse your data into.
   */
  public Importer(Class<T> clazz) {
    this.clazz = clazz;
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

  protected abstract void importModels(Datastore ds) throws LungsException;

}
