package model;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

import ml.ROIGenerator;
import util.MongoHelper;

/**
 * Model used to hold information about single region of interest in a matrix.
 *
 * @author Stuart Clark
 */
public class ROI {

  private static final Datastore DS = MongoHelper.getDataStore();

  public enum Class {
    NODULE, NON_NODULE
  }

  public enum Set {
    TRAIN, TEST
  }

  @Id
  private ObjectId id;

  private List<Point> region;

  /**
   * The inclusive contour for the region. i.e. the points in the contour are included in the
   * region.
   */
  private List<Point> contour;

  @Indexed
  private String imageSopUID;

  @Indexed
  private String seriesInstanceUID;

  @Indexed
  private Class classification;

  /**
   * The set that the ROI belongs too i.e TRAIN or TEST
   */
  @Indexed
  private Set set;

  /**
   * The value for {@link config.Misc#MATCH_THRESHOLD} used when classifying the ROI see
   * {@link ROIGenerator}.
   */
  private Double matchThreshold;

  /**
   * The score obtained for the best match of this {@link ROI} to a {@link GroundTruth} in
   * {@link ml.ROIClassifier#classify(ROI)}.
   */
  @Indexed
  private Double matchScore;

  /**
   * The {@link ObjectId} for the best matching {@link GroundTruth} for this {@link ROI} was
   * matched. {@code null} if the {@link ROI} was not matched to any {@link GroundTruth}.
   */
  @Indexed
  private ObjectId groundTruth;

  /*
   * The follow fields are used as features for the classifier
   */

  private boolean juxtapleural;

  private Double meanIntensity;

  /**
   * A intensity histogram with few bins. Computed using {@link ml.feature.AllHists}.
   */
  private Histogram coarseHist;

  /**
   * A intensity histogram with a medium amount of bins. Computed using {@link ml.feature.AllHists}.
   */
  private Histogram medHist;

  /**
   * A intensity histogram with lots of bins. Computed using {@link ml.feature.AllHists}.
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
  private Integer area;

  /**
   * The minimum bounding circle computed using {@link ml.feature.MinCircle}.
   */
  private Circle minCircle;

  /**
   * The circularity of the {@link ROI} computed using {@link ml.feature.Circularity}.
   */
  private Double circularity;

  /**
   * The circularity of the {@link ROI} computed using {@link ml.feature.HuCircularity}.
   */
  private Double huCircularity;

  /**
   * The rotated rect that contains the a fitted ellipse computed using
   * {@link ml.feature.FitEllipse}.
   */
  private RotatedRect fitEllipse;


  /**
   * The minimum rotated rect that contains the {@link ROI}, computed using
   * {@link ml.feature.BoundingBox}.
   */
  private RotatedRect boundingBox;

  /**
   * A rotation and scale invariant comparison of the width and height of the
   * {@link ROI#boundingBox}.
   */
  private Double elongation;

  /**
   * A value between 1 and 0 describing how how convex the {@link ROI} is. 1 being the most convex
   * and 0 being the least.
   */
  private Double convexity;

  /**
   * The histogram for the Local Quaternary Pattern for the {@link ROI}.
   */
  private Histogram lqp;

  public ROI() {
    region = new ArrayList<>();
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
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

  public Integer getArea() {
    return area;
  }

  public void setArea(Integer area) {
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

  public Set getSet() {
    return set;
  }

  public void setSet(Set set) {
    this.set = set;
  }

  public RotatedRect getBoundingBox() {
    return boundingBox;
  }

  public void setBoundingBox(RotatedRect boundingBox) {
    this.boundingBox = boundingBox;
  }

  public Double getElongation() {
    return elongation;
  }

  public void setElongation(Double elongation) {
    this.elongation = elongation;
  }

  public Double getMatchScore() {
    return matchScore;
  }

  public void setMatchScore(Double matchScore) {
    this.matchScore = matchScore;
  }

  public GroundTruth getGroundTruth() {
    return DS.get(GroundTruth.class, groundTruth);
  }

  public void setGroundTruth(GroundTruth groundTruth) {
    this.groundTruth = groundTruth.getId();
  }

  public boolean isJuxtapleural() {
    return juxtapleural;
  }

  public void setJuxtapleural(boolean juxtapleural) {
    this.juxtapleural = juxtapleural;

  }

  public Double getCircularity() {
    return circularity;
  }

  public void setCircularity(Double circularity) {
    this.circularity = circularity;
  }

  public Double getConvexity() {
    return convexity;
  }

  public void setConvexity(Double convexity) {
    this.convexity = convexity;
  }

  public Double getHuCircularity() {
    return huCircularity;
  }

  public void setHuCircularity(Double huCircularity) {
    this.huCircularity = huCircularity;
  }

  public Histogram getLqp() {
    return lqp;
  }

  public void setLqp(Histogram lqp) {
    this.lqp = lqp;
  }
}
