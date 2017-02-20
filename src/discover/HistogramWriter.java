package discover;

import java.util.StringJoiner;

import model.Histogram;
import util.LungsException;

/**
 * Used to write {@link Histogram}s to csv files.
 *
 * @author Stuart Clark
 */
public abstract class HistogramWriter extends CsvWriter {

  protected static final int LOG_INTERVAL = 1000;
  protected static final int BINS = 256;

  public HistogramWriter() throws LungsException {
    super();
  }

  /**
   * Adds a line to the csv file for {@code hist}.
   *
   * @param hist the {@link Histogram} that should be written to the csv file.
   */
  protected void writeLine(Histogram hist) {
    StringJoiner joiner = new StringJoiner(",");
    for (double v : hist.getBins()) {
      joiner.add(String.valueOf(v));
    }
    writer.println(joiner.toString());
  }

}
