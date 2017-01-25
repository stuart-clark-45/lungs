package util;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;

/**
 * Helper class used to access the morphia {@link Datastore} used by the system.
 * 
 * @author Stuart Clark
 */
public class MongoHelper {

  private static Datastore DS;

  static {
    String dbName = "passwords";
    MongoClient mongo = new MongoClient();
    Morphia morphia = new Morphia();
    DS = morphia.createDatastore(mongo, dbName);
  }

  public static Datastore getDataStore() {
    return DS;
  }

}
