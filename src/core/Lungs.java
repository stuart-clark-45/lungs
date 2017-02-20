package core;

import static config.Segmentation.Opening;
import static config.Segmentation.THRESHOLD;
import static config.Segmentation.Filter.KERNEL_SIZE;
import static config.Segmentation.Filter.SIGMA_COLOUR;
import static config.Segmentation.Filter.SIGMA_SPACE;
import static config.Segmentation.Opening.HEIGHT;
import static config.Segmentation.Opening.WIDTH;
import static model.GroundTruth.Type.BIG_NODULE;
import static model.GroundTruth.Type.NON_NODULE;
import static model.GroundTruth.Type.SMALL_NODULE;
import static org.opencv.imgproc.Imgproc.LINE_4;
import static org.opencv.imgproc.Imgproc.MARKER_TILTED_CROSS;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static util.ConfigHelper.getInt;
import static util.MatUtils.getStackMats;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Annotation;
import ml.ArffGenerator;
import ml.FeatureEngine;
import ml.InstancesBuilder;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.ColourBGR;
import util.ConfigHelper;
import util.DataFilter;
import util.LungsException;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;
import util.PointUtils;
import vision.ROIExtractor;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class Lungs {

  private static final Logger LOGGER = LoggerFactory.getLogger(Lungs.class);

  /**
   * The size of the cross used for annotations.
   */
  private static final int CROSS_SIZE = 5;

  /**
   * The thickness of the cross used for annotations.
   */
  private static final int CROSS_THICKNESS = 1;

  /**
   * The value used to represent the foreground in segmented images.
   */
  public static final int FOREGROUND = 255;

  private Datastore ds;
  private final ROIExtractor extractor;

  /*
   * Parameters used during segmentation
   */
  private int sigmaColour;
  private int sigmaSpace;
  private int kernelSize;
  private int threshold;
  private int openingWidth;
  private int openingHeight;
  private int openingKernel;

  public Lungs() {
    this(getInt(SIGMA_COLOUR), getInt(SIGMA_SPACE), getInt(KERNEL_SIZE), getInt(THRESHOLD),
        getInt(WIDTH), getInt(HEIGHT), getInt(Opening.KERNEL));
  }

  public Lungs(int sigmaColour, int sigmaSpace, int kernelSize, int threshold, int openingWidth,
      int openingHeight, int openingKernel) {
    this.ds = MongoHelper.getDataStore();
    this.sigmaColour = sigmaColour;
    this.sigmaSpace = sigmaSpace;
    this.kernelSize = kernelSize;
    this.threshold = threshold;
    this.openingWidth = openingWidth;
    this.openingHeight = openingHeight;
    this.openingKernel = openingKernel;
    this.extractor = new ROIExtractor(FOREGROUND);
  }

  /**
   * @param slices the slices to annotations.
   * @param bgrs 3 channel {@link Mat}s for the {@code slices}.
   * @return the {@code bgrs} {@link Mat}s with the ground truth annotated upon then.
   */
  private List<Mat> groundTruth(List<CTSlice> slices, List<Mat> bgrs) {
    List<Mat> annotated = new ArrayList<>(slices.size());

    for (int i = 0; i < slices.size(); i++) {
      CTSlice slice = slices.get(i);
      Mat bgr = bgrs.get(i);
      annotate(bgr, slice);
      annotated.add(bgr);
    }

    return annotated;
  }

  /**
   * @param rois the list of {@link ROI}s for which you would like to find the one with the largest
   *        area (in pixels).
   * @return an {@link Optional#of(Object)} the largest {@link ROI} or an {@link Optional#empty()}
   *         if {@code rois} is an empty list.
   * @throws LungsException
   */
  public Optional<ROI> largest(List<ROI> rois) throws LungsException {
    return rois.stream().max(Comparator.comparingInt(roi -> roi.getRegion().size()));
  }

  /**
   * Annotate {@code bgr} with the appropriate annotations for the {@code slice} if they are allowed
   * by the system configuration (see ./conf/application.conf).
   *
   * @param bgr
   * @param slice
   */
  private void annotate(Mat bgr, CTSlice slice) {
    List<GroundTruth> groundTruths =
        ds.createQuery(GroundTruth.class).field("imageSopUID").equal(slice.getImageSopUID())
            .asList();

    for (GroundTruth gt : groundTruths) {
      annotate(bgr, gt);
    }

  }

  /**
   * Annotate {@code bgr} with the appropriate annotations for the {@code gt} if they are allowed by
   * the system configuration (see ./conf/application.conf).
   * 
   * @param bgr
   * @param gt
   */
  private void annotate(Mat bgr, GroundTruth gt) {
    GroundTruth.Type type = gt.getType();

    // Annotate big nodules
    if (type == BIG_NODULE && ConfigHelper.getBoolean(Annotation.BIG_NODULE)) {
      for (Point point : gt.getEdgePoints()) {
        bgr.put((int) point.y, (int) point.x, ColourBGR.RED);
      }

      // Annotate small nodules
    } else if (type == SMALL_NODULE && ConfigHelper.getBoolean(Annotation.SMALL_NODULE)) {
      Imgproc.drawMarker(bgr, gt.getCentroid(), new Scalar(ColourBGR.RED), MARKER_TILTED_CROSS,
          CROSS_SIZE, CROSS_THICKNESS, LINE_4);

      // Annotate non nodules
    } else if (type == NON_NODULE && ConfigHelper.getBoolean(Annotation.NON_NODULE)) {
      Imgproc.drawMarker(bgr, gt.getCentroid(), new Scalar(ColourBGR.GREEN), MARKER_TILTED_CROSS,
          CROSS_SIZE, 1, LINE_4);
    }

  }

  /**
   * // TODO complete this javadoc when method had been decided upon.
   *
   * @param original the {@link Mat}s to segment.
   * @return the segmented {@link Mat}s.
   */
  public List<Mat> segment(List<Mat> original) {
    int numMat = original.size();
    List<Mat> segmented = new ArrayList<>(numMat);

    for (Mat orig : original) {
      // Filter the image
      Mat filtered = MatUtils.similarMat(orig);
      Imgproc.bilateralFilter(orig, filtered, kernelSize, sigmaColour, sigmaSpace);

      // Segment it
      Mat seg = MatUtils.similarMat(filtered);
      Imgproc.threshold(orig, seg, threshold, FOREGROUND, THRESH_BINARY);

      // Apply opening
      Mat opened = MatUtils.similarMat(seg);
      Imgproc.morphologyEx(seg, opened, Imgproc.MORPH_OPEN,
          Imgproc.getStructuringElement(openingKernel, new Size(openingWidth, openingHeight)));

      segmented.add(opened);
    }

    return segmented;
  }

  public void paintROI(Mat bgr, ROI roi, double[] colour) {
    for (Point point : roi.getRegion()) {
      bgr.put((int) point.y, (int) point.x, colour);
    }
  }

  /**
   * @param segmented a binary segmented {@link Mat} where the foreground has intensity values of
   *        {@link Lungs#FOREGROUND}.
   * @return A list of all the the {@link ROI}s that should be processed further by the system.
   * @throws LungsException
   */
  public List<ROI> extractRois(Mat segmented) throws LungsException {
    List<ROI> rois = extractor.extract(segmented);
    // Remove the largest
    Optional<ROI> largest = largest(rois);
    largest.ifPresent(rois::remove);
    rois.parallelStream().forEach(roi -> roi.setContour(PointUtils.region2perim(roi.getRegion())));
    return rois;
  }

  public void assistance(CTStack stack) throws Exception {
    List<Mat> original = getStackMats(stack);

    // Segment the images
    LOGGER.info("Segmenting images");
    List<Mat> segmented = segment(original);

    // Train classifier
    LOGGER.info("Training classifier");
    ArffLoader loader = new ArffLoader();
    loader.setFile(new File(ArffGenerator.TRAIN_FILE));
    Instances trainingData = loader.getStructure();
    trainingData.setClassIndex(trainingData.numAttributes() - 1);
    Classifier classifier = new J48();
    classifier.buildClassifier(trainingData);

    // Create nodule predictions
    LOGGER.info("Creating nodule predictions for stack");
    FeatureEngine fEngine = new FeatureEngine();
    InstancesBuilder iBuilder = new InstancesBuilder(false);
    List<Mat> predictions =
        original.stream().map(Mat::clone).map(MatUtils::grey2BGR).collect(Collectors.toList());
    // For each slice
    for (int i = 0; i < segmented.size(); i++) {
      Mat seg = segmented.get(i);
      Mat orig = original.get(i);
      Mat predict = predictions.get(i);

      // Create Instances
      List<ROI> rois = extractRois(seg);
      rois.parallelStream().forEach(roi -> fEngine.computeFeatures(roi, orig));
      Instances instances = iBuilder.createSet("Slice Instances", rois.size());
      iBuilder.addInstances(instances, rois);
      Attribute classAttribute = instances.classAttribute();

      // Create predictions
      for (int j = 0; j < instances.size(); j++) {

        // Classify the instance
        Instance instance = instances.get(j);
        double v = classifier.classifyInstance(instance);
        if (v != 0.0) {
          LOGGER.info("instance value: " + v);
        }

        ROI.Class classification = ROI.Class.valueOf(classAttribute.value((int) v));

        // If nodule then annotate
        // TODO class labels appear to be backwards no idea why
        if (!classification.equals(ROI.Class.NODULE)) {
          LOGGER.info("Nodule Found!");
          paintROI(predict, rois.get(j), ColourBGR.GREEN);
        }

      }

      LOGGER.info((i + 1) + "/" + segmented.size() + " slices processed");
    }

    // Add ground truth
    List<Mat> annotated = groundTruth(stack.getSlices(), predictions);

    // Display Mats
    new MatViewer(original, annotated).display();
  }

  public void gtVsNoduleRoi(CTStack stack) {
    LOGGER.info("Loading Mats...");
    List<Mat> original = getStackMats(stack);
    List<Mat> bgr = MatUtils.grey2BGR(original);

    List<CTSlice> slices = stack.getSlices();

    LOGGER.info("Drawing detected nodules ...");
    int numSlice = slices.size();
    for (int i = 0; i < numSlice; i++) {
      CTSlice slice = slices.get(i);

      Query<ROI> rois =
          ds.createQuery(ROI.class).field("imageSopUID").equal(slice.getImageSopUID())
              .field("classification").equal(ROI.Class.NODULE);
      for (ROI roi : rois) {
        paintROI(bgr.get(i), roi, ColourBGR.GREEN);
      }

      LOGGER.info(i + 1 + "/" + numSlice + " processed");
    }

    LOGGER.info("Annotating ground truth");
    List<Mat> annotated = groundTruth(slices, bgr);

    LOGGER.info("Preparing to display Mats...");
    new MatViewer(original, annotated).display();
  }

  public void displaySegmented(CTStack stack) {
    LOGGER.info("Loading Mats...");
    List<Mat> original = getStackMats(stack);
    List<Mat> segmented = segment(original);
    new MatViewer(original, segmented).display();
  }

  /**
   * Should be run with the following VM args
   * -Djava.library.path=/usr/local/opt/opencv3/share/OpenCV/java -Xss515m -Xmx6g
   *
   * @param args
   * @throws LungsException
   */
  public static void main(String[] args) throws Exception {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Load the images
    LOGGER.info("Loading images");
    Datastore ds = MongoHelper.getDataStore();
    CTStack stack = DataFilter.get().test(ds.createQuery(CTStack.class)).get();

    Lungs lungs = new Lungs();
//    lungs.gtVsNoduleRoi(stack);
//    lungs.assistance(stack);
    lungs.displaySegmented(stack);
  }
}
