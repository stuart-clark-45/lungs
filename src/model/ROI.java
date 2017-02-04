package model;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.opencv.core.Point;

/**
 * Model used to hold information about single region of interest in a matrix.
 *
 * @author Stuart Clark
 */
public class ROI {

  @Id
  private ObjectId id;

  private List<Point> points;

  public ROI() {
    points = new ArrayList<>();
  }

  public ObjectId getId() {
    return id;
  }

  public List<Point> getPoints() {
    return points;
  }

  public void setPoints(List<Point> points) {
    this.points = points;
  }

  public void addPoint(Point point) {
    points.add(point);
  }

}
