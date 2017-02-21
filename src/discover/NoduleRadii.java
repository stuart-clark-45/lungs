package discover;

import static model.GroundTruth.Type.BIG_NODULE;

import java.util.List;
import java.util.StringJoiner;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.GroundTruth;
import util.LungsException;

/**
 * Used to create a csv file containing all the nodule radii as they are given in the ground truth.
 *
 * @author Stuart Clark
 */
public class NoduleRadii extends CsvWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoduleRadii.class);
  private static final String READING_NUMBER = "readingNumber";
  private static final int LOG_INTERVAL = 1000;

  public NoduleRadii() throws LungsException {
    super();
  }

  @Override
  protected String fileName() {
    return "nodule-radii.csv";
  }

  @Override
  public void writeToFile() throws LungsException {
    LOGGER.info("Running NoduleRadii...");

    List readingNumbers = ds.getCollection(GroundTruth.class).distinct(READING_NUMBER);

    LOGGER.info("There are " + readingNumbers.size() + " distinct reading numbers");

    for (Object obj : readingNumbers) {
      Integer readingNumber = (Integer) obj;

      // Get all the GTs for the reading number that are BIG_NODULES
      Query<GroundTruth> gts =
          filter.all(ds.createQuery(GroundTruth.class).field("type").equal(BIG_NODULE)
              .field(READING_NUMBER).equal(readingNumber));
      long numGts = gts.count();

      int counter = 0;
      StringJoiner joiner = new StringJoiner(",");
      for (GroundTruth gt : gts) {

        // Add each the radii to the joiner
        joiner.add(String.valueOf(gt.getMinRadius()));

        // Logging
        if (++counter % LOG_INTERVAL == 0) {
          LOGGER
              .info(counter + "/" + numGts + " gts processed for reading number " + readingNumber);
        }
      }

      // Print a line for the current reading number
      writer.println(joiner);
    }

    LOGGER.info("NoduleRadii has completed");
  }

  public static void main(String[] args) throws LungsException {
    new NoduleRadii().run();
  }

}
