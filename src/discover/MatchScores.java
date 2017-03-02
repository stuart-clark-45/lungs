package discover;

import java.util.StringJoiner;

import model.ROI;
import util.LungsException;

/**
 * Used to create a csv file containing all match scores for the {@link ROI}s.
 *
 * @author Stuart Clark
 */
public class MatchScores extends CsvWriter {

  public MatchScores() throws LungsException {
    super();
  }

  @Override
  protected String fileName() {
    return "match-scores.csv";
  }

  @Override
  public void writeToFile() throws LungsException {
    StringJoiner joiner = new StringJoiner(",");
    ds.createQuery(ROI.class).field("matchScore").notEqual(null)
        .forEach(roi -> joiner.add(String.valueOf(roi.getMatchScore())));
    writer.println(joiner.toString());
  }

  public static void main(String[] args) throws LungsException {
    new MatchScores().run();
  }

}
