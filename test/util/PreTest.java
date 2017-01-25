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
    LOGGER.info("Loading OpenCV...");
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    LOGGER.info("OpenCV Loaded");
  }

  /**
   * Creates a BlockJUnit4ClassRunner to run {@code klass}
   *
   * @param klass
   * @throws InitializationError if the test class is malformed.
   */
  public PreTest(Class<?> klass) throws InitializationError {
    super(klass);
  }

}
