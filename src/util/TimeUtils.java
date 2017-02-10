package util;

import java.util.concurrent.TimeUnit;

/**
 * A suite of utilities relating to timing.
 *
 * @author Stuart Clark
 */
public class TimeUtils {

  private TimeUtils (){
    // Hide constructor
  }

  public static String milliToString(long start) {
    long elapsed = System.currentTimeMillis() - start;
    return String.format(
        "%d min, %d sec",
        TimeUnit.MILLISECONDS.toMinutes(elapsed),
        TimeUnit.MILLISECONDS.toSeconds(elapsed)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)));
  }

}
