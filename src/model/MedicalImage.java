package model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Model used to hold information about single medical image.
 *
 * @author Stuart Clark
 */
@Entity
public class MedicalImage {

  @Id
  private ObjectId id;

  private String patientId;

  /**
   * The type of scan that was used to produce the image.
   */
  private String modality;

  /**
   * The manufacturer of the hardware used to produce the image.
   */
  private String manufacturer;

  /**
   * The mmodel of the hardware used to produce the image.
   */
  private String model;

  /**
   * Kilo-volts peak.
   */
  private Integer kVp;

  private Integer rows;

  private Integer columns;

  private Integer sliceThickness;

  /**
   * Used to order a set of images
   */
  private Integer imageNumber;

  private String studyInstanceUID;

  private String seriesInstanceUID;

  public ObjectId getId() {
    return id;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getModality() {
    return modality;
  }

  public void setModality(String modality) {
    this.modality = modality;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Integer getkVp() {
    return kVp;
  }

  public void setkVp(Integer kVp) {
    this.kVp = kVp;
  }

  public Integer getRows() {
    return rows;
  }

  public void setRows(Integer rows) {
    this.rows = rows;
  }

  public Integer getColumns() {
    return columns;
  }

  public void setColumns(Integer columns) {
    this.columns = columns;
  }

  public Integer getSliceThickness() {
    return sliceThickness;
  }

  public void setSliceThickness(Integer sliceThickness) {
    this.sliceThickness = sliceThickness;
  }

  public Integer getImageNumber() {
    return imageNumber;
  }

  public void setImageNumber(Integer imageNumber) {
    this.imageNumber = imageNumber;
  }

  public String getStudyInstanceUID() {
    return studyInstanceUID;
  }

  public void setStudyInstanceUID(String studyInstanceUID) {
    this.studyInstanceUID = studyInstanceUID;
  }

  public String getSeriesInstanceUID() {
    return seriesInstanceUID;
  }

  public void setSeriesInstanceUID(String seriesInstanceUID) {
    this.seriesInstanceUID = seriesInstanceUID;
  }

}
