package util;

import java.util.concurrent.TimeUnit;

/**
 * A suite of utilities relating to timing.
 *
 * @author Stuart Clark
 */
public class TimeUtils {

  private TimeUtils() {
    // Hide constructor
  }

  /**
   * @param start the time that timing was started in milliseconds
   * @return the time elapsed since {@code start} as a formatted string in hrs, mins and secs.
   */
  public static String elapsedTime(long start) {
    long elapsed = System.currentTimeMillis() - start;
    return milliToString(elapsed);
  }

  /**
   * @param time the time you want to format in milliseconds.
   * @return the time formatted to a string with hrs, mins and secs.
   */
  public static String milliToString(long time) {
    return String.format(
        "%d hrs %d min, %d sec",
        TimeUnit.MILLISECONDS.toHours(time),
        TimeUnit.MILLISECONDS.toMinutes(time)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
        TimeUnit.MILLISECONDS.toSeconds(time)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
  }

}
