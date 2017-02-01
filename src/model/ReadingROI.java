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
    NODULE, NON_NODULE
  }

  @Id
  private ObjectId id;

  private Type type;

  private Point centroid;

  /**
   * Used to link {@code this} to the corresponding {@code ImageSlice}.
   */
  private String imageSopUID;

  private List<Point> edgePoints;

  public ReadingROI(Type type) {
    this.type = type;
  }

  public ObjectId getId() {
    return id;
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

}
