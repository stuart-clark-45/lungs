package util;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;

/**
 * Helper class used to access the morphia {@link Datastore} used by the system.
 * 
 * @author Stuart Clark
 */
public class MongoHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(MongoHelper.class);

  private static Datastore DS;

  static {
    String dbName = (String) ConfigHelper.getProps().get("db");
    MongoClient mongo = new MongoClient();

    // Set the cursor timeout to be an hour
    int timeout = 60 * 60 * 1000;
    LOGGER.info("Setting cursor timeout to " + TimeUtils.milliToString(timeout));
    Map<String, Object> documentMap = new HashMap<>();
    documentMap.put("setParameter", 1);
    documentMap.put("cursorTimeoutMillis", timeout);
    Document result = mongo.getDatabase("admin").runCommand(new Document(documentMap));
    LOGGER.info(result.toJson());

    Morphia morphia = new Morphia();
    DS = morphia.createDatastore(mongo, dbName);
  }

  public static Datastore getDataStore() {
    return DS;
  }

}
