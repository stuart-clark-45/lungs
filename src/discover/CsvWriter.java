package discover;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.mongodb.morphia.Datastore;

import config.Misc;
import util.ConfigHelper;
import util.DataFilter;
import util.LungsException;
import util.MongoHelper;

/**
 * Should extended by discovery classes that are used to write csv files.
 *
 * @author Stuart Clark
 */
public abstract class CsvWriter {

  private static final String DIR = ConfigHelper.getString(Misc.CSV_DIR);

  protected final Datastore ds;
  protected final DataFilter filter;
  protected final PrintWriter writer;

  public CsvWriter() throws LungsException {
    ds = MongoHelper.getDataStore();
    filter = DataFilter.get();
    try {
      writer = new PrintWriter(DIR + "/" + fileName(), "UTF-8");
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw new LungsException("Failed to create print writer");
    }
  }

  /**
   * Call this method to start the CsvWriter.
   * 
   * @throws LungsException
   */
  public void run() throws LungsException {
    writeToFile();
    writer.close();
  }

  /**
   * @return the name of the csv file that you would like to write to.
   */
  protected abstract String fileName();

  /**
   * Convert the chosen data into csv and save a file using {@code writer.println()} etc. There is
   * no need to close the writer as it is already done in {@link CsvWriter#run()}.
   */
  public abstract void writeToFile() throws LungsException;

}
