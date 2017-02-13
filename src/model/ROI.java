package model;

import java.util.ArrayList;
import java.util.List;

import ml.ROIGenerator;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.opencv.core.Point;

/**
 * Model used to hold information about single region of interest in a matrix.
 *
 * @author Stuart Clark
 */
public class ROI {

  public enum Class {
    NODULE, NON_NODULE
  }

  @Id
  private ObjectId id;

  private List<Point> points;

  @Indexed
  private String imageSopUID;

  @Indexed
  private String seriesInstanceUID;

  private Double meanIntensity;

  private double[] intensityHistogram;

  @Indexed
  private Class classification;

  /**
   * The value for {@link config.Misc#MATCH_THRESHOLD} used when classifying the ROI see
   * {@link ROIGenerator}.
   */
  private Double matchThreshold;

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

  public String getImageSopUID() {
    return imageSopUID;
  }

  public void setImageSopUID(String imageSopUID) {
    this.imageSopUID = imageSopUID;
  }

  public Double getMeanIntensity() {
    return meanIntensity;
  }

  public void setMeanIntensity(Double meanIntensity) {
    this.meanIntensity = meanIntensity;
  }

  public Class getClassification() {
    return classification;
  }

  public void setClassification(Class classification) {
    this.classification = classification;
  }

  public Double getMatchThreshold() {
    return matchThreshold;
  }

  public void setMatchThreshold(Double matchThreshold) {
    this.matchThreshold = matchThreshold;
  }

  public String getSeriesInstanceUID() {
    return seriesInstanceUID;
  }

  public void setSeriesInstanceUID(String seriesInstanceUID) {
    this.seriesInstanceUID = seriesInstanceUID;
  }

  public double[] getIntensityHistogram() {
    return intensityHistogram;
  }

  public void setIntensityHistogram(double[] intensityHistogram) {
    this.intensityHistogram = intensityHistogram;
  }
  
}
