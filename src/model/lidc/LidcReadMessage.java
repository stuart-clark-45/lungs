package model.lidc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for top level element of the LIDC xml files.
 *
 * @author Stuart Clark
 */
@XmlRootElement(name = "LidcReadMessage")
public class LidcReadMessage {

  protected List<ReadingSession> readingSessions;

  public List<ReadingSession> getReadingSessions() {
    if (readingSessions == null) {
      readingSessions = new ArrayList<>();
    }
    return readingSessions;
  }

  public void setReadingSessions(List<ReadingSession> readingSessions) {
    this.readingSessions = readingSessions;
  }
}
