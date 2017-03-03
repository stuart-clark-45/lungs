package discover;

import java.util.Iterator;
import java.util.StringJoiner;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ROI;
import util.LungsException;

/**
 * Used to create a csv file containing all match scores for the {@link ROI}s.
 *
 * @author Stuart Clark
 */
public class MatchScores extends CsvWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatchScores.class);
  private static final int LOG_INTERVAL = 1000;

  public MatchScores() throws LungsException {
    super();
  }

  @Override
  protected String fileName() {
    return "match-scores.csv";
  }

  @Override
  public void writeToFile() throws LungsException {
    LOGGER.info("Running MatchScores...");

    StringJoiner joiner = new StringJoiner(",");

    // Find all the ROIs that have a non null matchScore
    LOGGER.info("Finding all the ROIs that have a non null matchScore...");
    Query<ROI> query = ds.createQuery(ROI.class).field("matchScore").notEqual(null);
    long numROI = query.count();

    // Iterate over the ROIs
    Iterator<ROI> iterator = query.iterator();
    int counter = 0;
    while (iterator.hasNext()) {
      // Add the match score of the ROI to the match score
      ROI roi = iterator.next();
      joiner.add(String.valueOf(roi.getMatchScore()));

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numROI + " ROIs processsed");
      }
    }

    // Write the results to the csv file
    LOGGER.info("Writing results to file...");
    writer.println(joiner.toString());

    LOGGER.info("Finished Running MatchScores");
  }

  public static void main(String[] args) throws LungsException {
    new MatchScores().run();
  }

}
