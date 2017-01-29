package model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

/**
 * Model used to hold information about single medical image.
 *
 * @author Stuart Clark
 */
@Entity
public class MedicalImage {

  @Id
  private ObjectId id;

  private String filePath;

  @Indexed
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

  @Indexed
  private String seriesInstanceUID;

  private Double sliceLocation;

  private Integer bitsAllocated;

  private Integer bitsStored;

  private Integer highBit;

  private Integer seriesNumber;

  public ObjectId getId() {
    return id;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
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

  public void setStudyInstanceUIDs(String studyInstanceUID) {
    this.studyInstanceUID = studyInstanceUID;
  }

  public String getSeriesInstanceUID() {
    return seriesInstanceUID;
  }

  public void setSeriesInstanceUID(String seriesInstanceUID) {
    this.seriesInstanceUID = seriesInstanceUID;
  }

  public Double getSliceLocation() {
    return sliceLocation;
  }

  public Integer getBitsAllocated() {
    return bitsAllocated;
  }

  public void setBitsAllocated(Integer bitsAllocated) {
    this.bitsAllocated = bitsAllocated;
  }

  public Integer getBitsStored() {
    return bitsStored;
  }

  public void setBitsStored(Integer bitsStored) {
    this.bitsStored = bitsStored;
  }

  public Integer getHighBit() {
    return highBit;
  }

  public void setHighBit(Integer highBit) {
    this.highBit = highBit;
  }

  public Integer getSeriesNumber() {
    return seriesNumber;
  }

  public void setSeriesNumber(Integer seriesNumber) {
    this.seriesNumber = seriesNumber;
  }

  public void setSliceLocation(Double sliceLocation) {
    this.sliceLocation = sliceLocation;
  }
}
