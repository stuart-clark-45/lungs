package core;

import generator.CTStackGenerator;
import importer.CTSliceImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to import and aggregate all of the data required for the system to be run.
 *
 * @author Stuart Clark
 */
public class DataPipeline {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataPipeline.class);

  public static void main(String[] args) {
    LOGGER.info("Running DataPineLine");

    new CTSliceImporter().run();
    new CTStackGenerator().run();

    LOGGER.info("DataPineLine complete.");
  }

}
