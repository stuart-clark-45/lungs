package model.lidc;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for header of the LIDC xml files.
 *
 * @author Stuart Clark
 */
@XmlRootElement(name = "ResponseHeader")
public class ResponseHeader {

  protected String StudyInstanceUID;

  public String getStudyInstanceUID() {
    return StudyInstanceUID;
  }

  public void setStudyInstanceUID(String studyInstanceUID) {
    StudyInstanceUID = studyInstanceUID;
  }

}
