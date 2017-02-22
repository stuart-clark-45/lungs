package core;

import static config.Segmentation.Opening;
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
import static util.ConfigHelper.getInt;
import static util.MatUtils.getStackMats;

import java.io.File;
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
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Annotation;
import config.Segmentation;
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
  private int openingWidth;
  private int openingHeight;
  private int openingKernel;

  public Lungs() {
    this(getInt(SIGMA_COLOUR), getInt(SIGMA_SPACE), getInt(KERNEL_SIZE),
        getInt(Segmentation.SURE_FG), getInt(Segmentation.SURE_BG), getInt(WIDTH), getInt(HEIGHT),
        getInt(Opening.KERNEL));
  }

  public Lungs(int sigmaColour, int sigmaSpace, int kernelSize, int sureFG, int sureBG,
      int openingWidth, int openingHeight, int openingKernel) {
    this.ds = MongoHelper.getDataStore();
    this.sigmaColour = sigmaColour;
    this.sigmaSpace = sigmaSpace;
    this.kernelSize = kernelSize;
    this.openingWidth = openingWidth;
    this.openingHeight = openingHeight;
    this.openingKernel = openingKernel;
    this.extractor = new ROIExtractor(sureFG, sureBG);
  }

  /**
   * Annotated {@code bgrs} with the ground truth.
   *
   * @param slices the slices to annotations.
   * @param bgrs 3 channel {@link Mat}s for the {@code slices} that will be annotated.
   */
  private void groundTruth(List<CTSlice> slices, List<Mat> bgrs) {
    for (int i = 0; i < slices.size(); i++) {
      CTSlice slice = slices.get(i);
      Mat bgr = bgrs.get(i);
      annotate(bgr, slice);
    }
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
  public List<ROI> extractRois(Mat original) {
    // Filter the image
    Mat filtered = MatUtils.similarMat(original);
    Imgproc.bilateralFilter(original, filtered, kernelSize, sigmaColour, sigmaSpace);

    // Extract ROIs and return then
    return extractor.extractROIs(filtered);
  }

  public void paintROI(Mat bgr, ROI roi, double[] colour) {
    for (Point point : roi.getRegion()) {
      bgr.put((int) point.y, (int) point.x, colour);
    }
  }

  public void assistance(CTStack stack) throws Exception {
    List<Mat> mats = getStackMats(stack);

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
    List<Mat> annotated =
        mats.stream().map(Mat::clone).map(MatUtils::grey2BGR).collect(Collectors.toList());
    // For each slice
    for (int i = 0; i < mats.size(); i++) {
      Mat mat = mats.get(i);
      Mat predict = annotated.get(i);

      // Create Instances
      List<ROI> rois = extractRois(mat);
      rois.parallelStream().forEach(roi -> fEngine.computeFeatures(roi, mat));
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

      LOGGER.info((i + 1) + "/" + mats.size() + " slices processed");
    }

    // Add ground truth
    groundTruth(stack.getSlices(), annotated);

    // Display Mats
    new MatViewer(mats, annotated).display();
  }

  public void gtVsNoduleRoi(CTStack stack) {
    LOGGER.info("Loading Mats...");
    List<Mat> original = getStackMats(stack);
    List<Mat> annotated = MatUtils.grey2BGR(original);

    List<CTSlice> slices = stack.getSlices();

    LOGGER.info("Drawing detected nodules ...");
    int numSlice = slices.size();
    for (int i = 0; i < numSlice; i++) {
      CTSlice slice = slices.get(i);

      Query<ROI> rois =
          ds.createQuery(ROI.class).field("imageSopUID").equal(slice.getImageSopUID())
              .field("classification").equal(ROI.Class.NODULE);
      for (ROI roi : rois) {
        paintROI(annotated.get(i), roi, ColourBGR.GREEN);
      }

      LOGGER.info(i + 1 + "/" + numSlice + " processed");
    }

    LOGGER.info("Annotating ground truth");
    groundTruth(slices, annotated);

    LOGGER.info("Preparing to display Mats...");
    new MatViewer(original, annotated).display();
  }

  public void annotatedSegmented(CTStack stack) {
    LOGGER.info("Loading Mats...");
    List<Mat> original = getStackMats(stack);

    // Create bgr copies that can be annotated
    List<Mat> annotated =
        original.parallelStream().map(MatUtils::grey2BGR).collect(Collectors.toList());

    // Paint ROIs to annotated Mats
    for (int i = 0; i < original.size(); i++) {
      Mat orig = original.get(i);
      Mat anno = annotated.get(i);
      for (ROI roi : extractor.extractROIs(orig)) {
        paintROI(anno, roi, ColourBGR.GREEN);
      }
    }

    // Paint ground truth to annotated Mats
    groundTruth(stack.getSlices(), annotated);

    // Display annotated and original Mats
    new MatViewer(original, annotated).display();
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
    // lungs.gtVsNoduleRoi(stack);
    // lungs.assistance(stack);
    lungs.annotatedSegmented(stack);
  }
}
