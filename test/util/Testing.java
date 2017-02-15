package util;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Mode;

/**
 * Runs code to be executed before all tests. And provides generic testing utilities.
 *
 * @author Stuart Clark.
 */
public class Testing extends BlockJUnit4ClassRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testing.class);

  static {
    // Load OpenCV
    LOGGER.info("Loading OpenCV...");
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    LOGGER.info("OpenCV Loaded");

    // Set up database
    String dbName = "junitdb";
    LOGGER.info("Using " + dbName + "as database");
    ConfigHelper.getProps().put("db", dbName);
    drop();

    // Set application mode
    ConfigHelper.getProps().put(Mode.KEY, Mode.Value.TEST.name());
    LOGGER.info("Config set up");
  }

  /**
   * Creates a BlockJUnit4ClassRunner to run {@code clazz}
   *
   * @param clazz
   * @throws InitializationError if the test class is malformed.
   */
  public Testing(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

  /**
   * Drop the database.
   */
  public static void drop() {
    MongoHelper.getDataStore().getDB().dropDatabase();
  }

}
