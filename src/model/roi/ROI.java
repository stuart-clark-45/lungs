package model.roi;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

import ml.ROIGenerator;

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

  private List<Point> region;

  private List<Point> contour;

  @Indexed
  private String imageSopUID;

  @Indexed
  private String seriesInstanceUID;

  @Indexed
  private Class classification;

  /**
   * The value for {@link config.Misc#MATCH_THRESHOLD} used when classifying the ROI see
   * {@link ROIGenerator}.
   */
  private Double matchThreshold;

  /*
   * The follow fields are used as features for the classifier
   */

  private Double meanIntensity;

  /**
   * A intensity histogram with few bins. Computed using {@link ml.feature.CoarseHist}.
   */
  private Histogram coarseHist;

  /**
   * A intensity histogram with a medium amount of bins. Computed using {@link ml.feature.MedHist}.
   */
  private Histogram medHist;

  /**
   * A intensity histogram with lots of bins. Computed using {@link ml.feature.FineHist}.
   */
  private Histogram fineHist;

  /**
   * The length of the contour calculated using {@link ml.feature.Perimeter}. This value may not be
   * the same as {@code contour.size()}.
   */
  private int perimLength;

  /**
   * The area of the region calculated using {@link ml.feature.Area}. This value may not be the same
   * as {@code region.size()}.
   */
  private int area;

  /**
   * The minimum bounding circle computed using {@link ml.feature.MinCircle}.
   */
  private Circle minCircle;

  /**
   * The rotated rect that contains the a fitted ellipse computed using
   * {@link ml.feature.FitEllipse}.
   */
  private RotatedRect fitEllipse;

  public ROI() {
    region = new ArrayList<>();
  }

  public ObjectId getId() {
    return id;
  }

  public List<Point> getRegion() {
    return region;
  }

  public void setRegion(List<Point> region) {
    this.region = region;
  }

  public void addPoint(Point point) {
    region.add(point);
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

  public List<Point> getContour() {
    return contour;
  }

  public void setContour(List<Point> contour) {
    this.contour = contour;
  }

  public int getPerimLength() {
    return perimLength;
  }

  public void setPerimLength(int perimLength) {
    this.perimLength = perimLength;
  }

  public int getArea() {
    return area;
  }

  public void setArea(int area) {
    this.area = area;
  }

  public Circle getMinCircle() {
    return minCircle;
  }

  public void setMinCircle(Circle minCircle) {
    this.minCircle = minCircle;
  }

  public RotatedRect getFitEllipse() {
    return fitEllipse;
  }

  public void setFitEllipse(RotatedRect fitEllipse) {
    this.fitEllipse = fitEllipse;
  }

  public Histogram getCoarseHist() {
    return coarseHist;
  }

  public void setCoarseHist(Histogram coarseHist) {
    this.coarseHist = coarseHist;
  }

  public Histogram getMedHist() {
    return medHist;
  }

  public void setMedHist(Histogram medHist) {
    this.medHist = medHist;
  }

  public Histogram getFineHist() {
    return fineHist;
  }

  public void setFineHist(Histogram fineHist) {
    this.fineHist = fineHist;
  }

}
