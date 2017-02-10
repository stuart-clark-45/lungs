package data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ml.FeatureEngine;

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
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    LOGGER.info("Running DataPineLine");
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
    LOGGER.info("Finished running importers. Time elapsed: " + timeToString(start));

    // Generators (Must be run after importers)
    LOGGER.info("Running Generators...");
    futures = new ArrayList<>();
    futures.add(es.submit(new CTStackGenerator()));
    for (Future f : futures) {
      f.get();
    }
    // Run on it's own as has internal threading
    new ROIGenerator(es).run();
    LOGGER.info("Finished running Generators. Time elapsed: " + timeToString(start));

    // Feature Engine run on it's own as has internal threading
    new FeatureEngine().run(es);

    LOGGER.info("DataPineLine complete. Time elapsed: " + timeToString(start));
  }

  private static String timeToString(long start) {
    long elapsed = System.currentTimeMillis() - start;
    return String.format(
        "%d min, %d sec",
        TimeUnit.MILLISECONDS.toMinutes(elapsed),
        TimeUnit.MILLISECONDS.toSeconds(elapsed)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)));
  }

}
