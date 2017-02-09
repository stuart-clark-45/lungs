package model.lidc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for header of the LIDC xml files.
 *
 * @author Stuart Clark
 */
@XmlRootElement(name = "ResponseHeader")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResponseHeader {

  @XmlElement(name = "SeriesInstanceUid")
  protected String seriesInstanceUID;

  public String getSeriesInstanceUID() {
    return seriesInstanceUID;
  }

  public void setSeriesInstanceUID(String seriesInstanceUID) {
    this.seriesInstanceUID = seriesInstanceUID;
  }

}
