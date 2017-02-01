package model;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.opencv.core.Point;

/**
 * Used to hold information about a single region of interest identified by a radiologist(s) in a
 * given ground truth.
 *
 * @author Stuart Clark
 */
@Entity
public class ReadingROI {

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
   * This id will be the same for all {@link ReadingROI}s that correspond to the same nodule or
   * non-nodule.
   */
  private ObjectId groupId;

  private Type type;

  private Point centroid;

  /**
   * True if the {@code edgePoints} are included in area/volume defined, false otherwise.
   */
  private boolean inclusive;

  /**
   * Used to link {@code this} to the corresponding {@code ImageSlice}.
   */
  private String imageSopUID;

  private List<Point> edgePoints;

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

}