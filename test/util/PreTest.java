package util;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs code to be exectuded before all tests
 *
 * @author Stuart Clark.
 */
public class PreTest extends BlockJUnit4ClassRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreTest.class);

  static {
    // Load OpenCV
    LOGGER.info("Loading OpenCV...");
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    LOGGER.info("OpenCV Loaded");

    // Set up database
    String dbName = "junitdb";
    LOGGER.info("Using " + dbName + "as database");
    ConfigHelper.getProps().put("db", dbName);
    MongoHelper.getDataStore().getDB().dropDatabase();

    // Set application mode
    ConfigHelper.getProps().put("mode", "test");
    LOGGER.info("Config set up");
  }

  /**
   * Creates a BlockJUnit4ClassRunner to run {@code clazz}
   *
   * @param clazz
   * @throws InitializationError if the test class is malformed.
   */
  public PreTest(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

}
