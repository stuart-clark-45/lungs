package importer;

import java.util.concurrent.Callable;

import org.mongodb.morphia.Datastore;

import com.mongodb.DBCollection;

import util.LungsException;
import util.MongoHelper;

/**
 * Should be extended by classes that are used to import data into MongoDB
 *
 * @author Stuart Clark
 */
public abstract class Importer<T> implements Callable<Void> {

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
  public Void call() throws Exception {
    Datastore ds = MongoHelper.getDataStore();
    DBCollection collection = ds.getCollection(clazz);

    collection.drop();
    collection.dropIndexes();

    importModels(ds);

    ds.ensureIndexes(clazz);

    return null;
  }

  protected abstract void importModels(Datastore ds) throws LungsException;

}
