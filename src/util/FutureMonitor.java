package util;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intended to be used to log the progress of a large number of {@link Future}s
 *
 * @author Stuart Clark
 */
public class FutureMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FutureMonitor.class);

  /**
   * The runnable used to log the progress of the futures.
   */
  private final ProgressLogger progress;

  /**
   * The futures to monitor.
   */
  private List<Future> futures;

  public FutureMonitor(List<Future> futures) {
    // logInterval of 10s
    this(futures, 10000);
  }

  /**
   * @param futures the futures to monitor
   * @param logInterval the logging interval in milliseconds.
   */
  public FutureMonitor(List<Future> futures, long logInterval) {
    this.futures = futures;
    this.progress = new ProgressLogger(futures, logInterval);
  }

  /**
   * Commences monitoring of the {@link Future}s. This method will block until all of the
   * {@link Future}s are done.
   */
  public void monitor() {
    // Create and start logging thread
    new Thread(progress).start();

    // Wait for all the futures to complete
    for (Future future : futures) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error("Failed to get Future", e);
      }
    }

    // Stop the logging thread
    progress.stop();
  }

  /**
   * Set the string that should be used when logging e.g. "futures have completed".
   */
  public void setLogString(String logString) {
    progress.setLogString(logString);
  }

  /**
   * A {@link Runnable} used to log the progress of the {@link Future}s.
   */
  private static class ProgressLogger implements Runnable {

    /**
     * The futures to monitor.
     */
    private List<Future> futures;

    /**
     * The logging interval in milliseconds.
     */
    private long logInterval;

    /**
     * The string that should be used when logging e.g. "futures have completed".
     */
    private String logString;

    private boolean running;

    ProgressLogger(List<Future> futures, long logInterval) {
      this.futures = futures;
      this.logInterval = logInterval;
      this.running = true;
    }

    void setLogString(String logString) {
      this.logString = logString;
    }

    @Override
    public void run() {
      int counter = 0;
      while (running && counter < futures.size()) {
        // Count the number of futures that are complete
        counter = 0;
        for (Future future : futures) {
          if (future.isDone()) {
            counter++;
          }
        }

        // Logging
        LOGGER.info(counter + "/" + futures.size() + " " + logString);

        // Sleep for logInterval milliseconds
        try {
          Thread.sleep(logInterval);
        } catch (InterruptedException e) {
          LOGGER.error("Logging thread interrupted", e);
        }
      }
    }

    void stop() {
      running = false;
    }
  }

}
