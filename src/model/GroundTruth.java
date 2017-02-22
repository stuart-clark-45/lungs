package model;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.opencv.core.Point;

/**
 * Used to hold information about a single region of interest identified by a radiologist(s).
 *
 * @author Stuart Clark
 */
@Entity
public class GroundTruth {

  public enum Type {
    /**
     * Greater than or equal to 3mm.
     */
    BIG_NODULE,

    /**
     * Less than 3mm.
     */
    SMALL_NODULE,

    /**
     * Greater than or equal to 3mm. If smaller than not included.
     */
    NON_NODULE
  }

  @Id
  private ObjectId id;

  /**
   * This id will be the same for all {@link GroundTruth}s that correspond to the same nodule or
   * non-nodule.
   */
  @Indexed
  private ObjectId groupId;

  @Indexed
  private Type type;

  private Point centroid;

  /**
   * True if the {@code edgePoints} are included in area/volume defined, false otherwise.
   */
  private boolean inclusive;

  /**
   * Used to link {@code this} to the corresponding {@code ImageSlice}.
   */
  @Indexed
  private String imageSopUID;

  private List<Point> edgePoints;

  private List<Point> region;

  @Indexed
  private String seriesInstanceUID;

  /**
   * If multiple readings have been made for the same image this value can be used to distinguish
   * them.
   */
  @Indexed
  private Integer readingNumber;

  /**
   * The radius of the minimum fitting circle for the {@code region} used to define this
   * {@link GroundTruth}.
   */
  private double minRadius;

  /**
   * True if this ground truth was matched to an ROI (nodules only).
   */
  @Indexed
  private boolean matchedToRoi;

  /**
   * The roi this ground truth was matched to.
   */
  private ROI roi;

  public ObjectId getId() {
    return id;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public Point getCentroid() {
    return centroid;
  }

  public void setCentroid(Point centroid) {
    this.centroid = centroid;
  }

  public String getImageSopUID() {
    return imageSopUID;
  }

  public void setImageSopUID(String imageSopUID) {
    this.imageSopUID = imageSopUID;
  }

  public List<Point> getEdgePoints() {
    return edgePoints;
  }

  public void setEdgePoints(List<Point> edgePoints) {
    this.edgePoints = edgePoints;
  }

  public ObjectId getGroupId() {
    return groupId;
  }

  public void setGroupId(ObjectId groupId) {
    this.groupId = groupId;
  }

  public boolean isInclusive() {
    return inclusive;
  }

  public void setInclusive(boolean inclusive) {
    this.inclusive = inclusive;
  }

  public List<Point> getRegion() {
    return region;
  }

  public void setRegion(List<Point> region) {
    this.region = region;
  }

  public String getSeriesInstanceUID() {
    return seriesInstanceUID;
  }

  public void setSeriesInstanceUID(String seriesInstanceUID) {
    this.seriesInstanceUID = seriesInstanceUID;
  }

  public Integer getReadingNumber() {
    return readingNumber;
  }

  public void setReadingNumber(Integer readingNumber) {
    this.readingNumber = readingNumber;
  }

  public double getMinRadius() {
    return minRadius;
  }

  public void setMinRadius(double minRadius) {
    this.minRadius = minRadius;
  }

  public boolean isMatchedToRoi() {
    return matchedToRoi;
  }

  public void setMatchedToRoi(boolean matchedToRoi) {
    this.matchedToRoi = matchedToRoi;
  }

  public ROI getRoi() {
    return roi;
  }

  public void setRoi(ROI roi) {
    this.roi = roi;
  }
  
}
