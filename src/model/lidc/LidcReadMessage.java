package model.lidc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for top level element of the LIDC xml files.
 *
 * @author Stuart Clark
 */
@XmlRootElement(name = "LidcReadMessage")
public class LidcReadMessage {

  protected List<ReadingSession> readingSessions;

  @XmlElement(name = "ResponseHeader")
  private ResponseHeader responseHeader;

  public List<ReadingSession> getReadingSessions() {
    if (readingSessions == null) {
      readingSessions = new ArrayList<>();
    }
    return readingSessions;
  }

  public void setReadingSessions(List<ReadingSession> readingSessions) {
    this.readingSessions = readingSessions;
  }

  public ResponseHeader getResponseHeader() {
    return responseHeader;
  }

  public void setResponseHeader(ResponseHeader responseHeader) {
    this.responseHeader = responseHeader;
  }

}
