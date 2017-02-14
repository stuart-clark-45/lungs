package data;

import static util.TimeUtils.milliToString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to import and aggregate all of the data and perform any pre-computation required for the
 * system to be run.
 *
 * @author Stuart Clark
 */
public class DataPipeline {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataPipeline.class);

  private DataPipeline() {
    // Hide the constructor
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    LOGGER.info("Running DataPipeLine");
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    long start = System.currentTimeMillis();

    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Importers
    LOGGER.info("Running Importers...");
    List<Future> futures = new ArrayList<>();
    futures.add(es.submit(new CTSliceImporter()));
    futures.add(es.submit(new GroundTruthImporter()));
    for (Future f : futures) {
      f.get();
    }
    LOGGER.info("Finished running importers. Time elapsed: " + milliToString(start));

    // Generators (Must be run after importers)
    LOGGER.info("Running Generators...");
    futures = new ArrayList<>();
    futures.add(es.submit(new CTStackGenerator()));
    for (Future f : futures) {
      f.get();
    }

    LOGGER.info("DataPipeLine complete. Time elapsed: " + milliToString(start));
  }

}
