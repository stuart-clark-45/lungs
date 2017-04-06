package ml;

import static util.TimeUtils.elapsedTime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import discover.ROIClassStats;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to run all the code required to produce arff files that can be used by
 * {@link weka.classifiers.Classifier}s.
 *
 * @author Stuart Clark
 */
public class MLPipeline {

  private static final Logger LOGGER = LoggerFactory.getLogger(MLPipeline.class);

  private MLPipeline() {
    // Hide the constructorx
  }

  public static void main(String[] args) throws Exception {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    LOGGER.info("Running MLPipeLine");
    long start = System.currentTimeMillis();
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    new ROIGenerator(es).run();
    LOGGER.info("ROIGenerator finished, Time elapsed: " + elapsedTime(start));

    ROIClassStats.main(new String[0]);
    LOGGER.info("ROIClassStats finished, Time elapsed: " + elapsedTime(start));

    new FeatureEngine().run(es);
    LOGGER.info("FeatureEngine finished, Time elapsed: " + elapsedTime(start));

    new ArffGenerator().run();
    LOGGER.info("ArffGenerator finished, Time elapsed: " + elapsedTime(start));

    LOGGER.info("MLPipeLine complete, Time elapsed: " + elapsedTime(start));
  }

}
