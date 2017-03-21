package core;

import static config.Segmentation.Blob.DOG_THRESH;
import static config.Segmentation.Blob.GRADIENT_THRESH;
import static config.Segmentation.Blob.NEIGHBOURHOOD_DEPTH;
import static config.Segmentation.Blob.NEIGHBOURHOOD_HEIGHT;
import static config.Segmentation.Blob.NEIGHBOURHOOD_WIDTH;
import static config.Segmentation.Blob.NUM_SIGMA;
import static config.Segmentation.Filter.KERNEL_SIZE;
import static config.Segmentation.Filter.SIGMA_COLOUR;
import static config.Segmentation.Filter.SIGMA_SPACE;
import static model.GroundTruth.Type.BIG_NODULE;
import static model.GroundTruth.Type.NON_NODULE;
import static model.GroundTruth.Type.SMALL_NODULE;
import static org.opencv.imgproc.Imgproc.LINE_4;
import static org.opencv.imgproc.Imgproc.MARKER_TILTED_CROSS;
import static util.ConfigHelper.getInt;
import static util.MatUtils.getStackMats;
import static util.MatUtils.put;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
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
import ml.feature.MinCircle;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.KeyPoint;
import model.ROI;
import util.ColourBGR;
import util.ConfigHelper;
import util.DataFilter;
import util.LungsException;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;
import util.PointUtils;
import vision.BilateralFilter;
import vision.BlobDetector;
import vision.ROIExtractor;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

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

  /**
   * The maximum size of a nodule in the data set provided.
   */
  private static final double MAX_NODULE_RADIUS = 35.2279;

  /**
   * The minimum size of a nodule in the data set provided.
   */
  private static final double MIN_NODULE_RADIUS = 1.0;

  private Datastore ds;
  private final ROIExtractor extractor;
  private final BilateralFilter filter;
  private BlobDetector blobDetector;

  public Lungs(BilateralFilter filter, ROIExtractor extractor, BlobDetector blobDetector) {
    this.ds = MongoHelper.getDataStore();
    this.filter = filter;
    this.extractor = extractor;
    this.blobDetector = blobDetector;
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
   * @throws LungsException TODO remove
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
        put(bgr, point, ColourBGR.RED);
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
  @SuppressWarnings("ConstantConditions")
  public List<ROI> extractRois(Mat original) {
    // Filter and extract ROIs
    List<ROI> rois = extractor.extractROIs(filter.filter(original));

    // Get largest ROI
    // TODO use largest(..)
    ROI largest = null;
    int maxSize = -1;
    for (ROI roi : rois) {
      int size = roi.getRegion().size();
      if (size > maxSize) {
        largest = roi;
        maxSize = size;
      }
    }

    // Create a mat with just the largest ROI in
    Mat largestMat = MatUtils.similarMat(original);
    for (Point point : largest.getRegion()) {
      put(largestMat, point, FOREGROUND);
    }

    // Create a set of all the points that lie withing the lungs cavities. Also includes some other
    // small cavities in the CT slice (such as the spinal cord cavity) which we are not actually
    // interested in but should make little difference to the final result.
    List<MatOfPoint> cavities = internalContours(largestMat);
    Mat cavityMat = MatUtils.similarMat(original);
    Imgproc.fillPoly(cavityMat, cavities, new Scalar(FOREGROUND));
    Set<Point> validPoints = maskPoints(cavityMat);

    // Filter out ROIs that do not occur inside the cavities. The remaining ROIs should be solitary
    // nodules and false positives.
    rois =
        rois.parallelStream().filter(roi -> validPoints.containsAll(roi.getRegion()))
            .collect(Collectors.toList());

    // Extract the Juxtapleural ROIs
    rois.addAll(extractJuxtapleural(largestMat, cavities, original));

    // Compute the contours for the ROIs
    rois.parallelStream()
        .forEach(roi -> roi.setContour(PointUtils.region2Contour(roi.getRegion())));

    /*
     * Remove any ROIs that are too big or too small to be a nodule. By using the MAX_NODULE_RADIUS
     * some abnormally big nodules could be ignored here. However if they are very large they are
     * likely to be visible over many slices where they will have a cross section with a reduced
     * radius. As such they should still be detectable by the system.
     */
    return rois.parallelStream().filter(roi -> {
      try {
        new MinCircle().compute(roi, original);
        double radius = roi.getMinCircle().getRadius();
        return radius >= MIN_NODULE_RADIUS && radius <= MAX_NODULE_RADIUS;
      } catch (LungsException e) {
        LOGGER.error("Failed to compute min circle for ROI", e);
        return true;
      }
    }).collect(Collectors.toList());

  }

  private List<MatOfPoint> internalContours(Mat largestRoi) {
    // Create list of internal contours for the ROI
    List<MatOfPoint> contours = new ArrayList<>();
    Imgproc.findContours(largestRoi, contours, new Mat(), Imgproc.RETR_TREE,
        Imgproc.CHAIN_APPROX_NONE);
    // Removed external contour
    contours.remove(0);

    return contours;
  }

  private Set<Point> maskPoints(Mat mask) {
    Mat labels = MatUtils.similarMat(mask);
    Imgproc.connectedComponents(mask, labels);
    Set<Point> maskPoints = new HashSet<>();
    ROIExtractor.labelsToROIs(labels).forEach(roi -> maskPoints.addAll(roi.getRegion()));
    return maskPoints;
  }

  private List<ROI> extractJuxtapleural(Mat largestRoi, List<MatOfPoint> contours, Mat original) {
    // Create a list of convex hulls for the contours
    List<MatOfPoint> hulls = new ArrayList<>();
    for (MatOfPoint contour : contours) {

      // Find the convex hull for the contour
      MatOfInt hull = new MatOfInt();
      Imgproc.convexHull(contour, hull);

      // Convert MatOfInt to MatOfPoint
      MatOfPoint matOfPoint = new MatOfPoint();
      matOfPoint.create((int) hull.size().height, 1, CvType.CV_32SC2);
      for (int i = 0; i < hull.size().height; i++) {
        int index = (int) hull.get(i, 0)[0];
        double[] point = new double[] {contour.get(index, 0)[0], contour.get(index, 0)[1]};
        matOfPoint.put(i, 0, point);
      }

      // Add to list
      hulls.add(matOfPoint);
    }

    // Create a set of points that could possibly contain juxtapleural nodules
    Mat hullsMat = MatUtils.similarMat(original);
    // Draw the convex hulls
    Imgproc.fillPoly(hullsMat, hulls, new Scalar(FOREGROUND));
    // Invert the mat to create the invertedHulls
    Mat invertedHulls = MatUtils.similarMat(hullsMat);
    Core.bitwise_not(hullsMat, invertedHulls);
    // Subtract the invertedHulls to largestROI to create the mask
    Mat mask = MatUtils.similarMat(invertedHulls);
    Core.subtract(largestRoi, invertedHulls, mask);
    Set<Point> validPoints = maskPoints(mask);

    List<KeyPoint> keyPoints = blobDetector.detect(original);
    Mat blobMat = MatUtils.similarMat(original);
    for (KeyPoint keyPoint : keyPoints) {
      if (validPoints.contains(keyPoint.getPoint())) {
        Imgproc.circle(blobMat, keyPoint.getPoint(), (int) keyPoint.getSigma() * 2, new Scalar(
            FOREGROUND), -1);
      }
    }

    // Extract ROIs and return
    Mat labels = MatUtils.similarMat(blobMat);
    Imgproc.connectedComponents(blobMat, labels);
    List<ROI> rois = ROIExtractor.labelsToROIs(labels);
    rois.forEach(roi -> roi.setJuxtapleural(true));
    return rois;
  }

  public static void paintROI(Mat bgr, ROI roi, double[] colour) {
    for (Point point : roi.getRegion()) {
      put(bgr, point, colour);
    }
  }

  public void assistance(CTStack stack) throws Exception {
    List<Mat> mats = getStackMats(stack);

    // Train classifier
    LOGGER.info("Training classifier");
    Instances trainingData = ConverterUtils.DataSource.read(ArffGenerator.TRAIN_FILE);
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
        ROI.Class classification = ROI.Class.valueOf(classAttribute.value((int) v));

        // If nodule then annotate green else annotate orange
        if (classification.equals(ROI.Class.NODULE)) {
          LOGGER.info("Nodule Found!");
          paintROI(predict, rois.get(j), ColourBGR.GREEN);
        } else {
          paintROI(predict, rois.get(j), ColourBGR.ORANGE);
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
    LOGGER.info("Creating BGR Mats for annotations...");
    List<Mat> annotated =
        original.parallelStream().map(MatUtils::grey2BGR).collect(Collectors.toList());

    // Paint ROIs to annotated Mats
    LOGGER.info("Painting Mats with ROIs...");
    for (int i = 0; i < original.size(); i++) {
      Mat orig = original.get(i);
      Mat anno = annotated.get(i);
      for (ROI roi : extractRois(orig)) {
        paintROI(anno, roi, ColourBGR.GREEN);
      }
      LOGGER.info(i + 1 + "/" + original.size() + " have had ROIs painted on");
    }

    // Paint ground truth to annotated Mats
    LOGGER.info("Creating BGR Mats for ground truth...");
    List<Mat> groundTruth =
        original.parallelStream().map(MatUtils::grey2BGR).collect(Collectors.toList());
    LOGGER.info("Paining Mats with ground truth...");
    groundTruth(stack.getSlices(), groundTruth);

    // Display annotated and original Mats
    LOGGER.info("Preparing to display...");
    MatViewer matViewer = new MatViewer(groundTruth, annotated);
    LOGGER.info("Displaying Mats now");
    matViewer.display();
  }

  public void roiContours(CTStack stack) {
    LOGGER.info("Loading Mats...");
    List<Mat> original = getStackMats(stack);

    LOGGER.info("Drawing contours on Mats...");
    List<Mat> annotated = new ArrayList<>();
    for (int i = 0; i < original.size(); i++) {
      LOGGER.info(i + "/" + original.size() + " processed");

      Mat mat = original.get(i);

      Mat bgr = MatUtils.grey2BGR(mat);
      for (ROI roi : extractRois(mat)) {
        paintROI(bgr, roi, ColourBGR.GREEN);
        for (Point point : roi.getContour()) {
          put(bgr, point, ColourBGR.RED);
        }
      }

      annotated.add(bgr);
    }

    LOGGER.info("Preparing to display Mats...");
    new MatViewer(annotated).display();
  }

  public void blobs(CTStack ctStack) {
    List<Mat> mats = MatUtils.getStackMats(ctStack);
    List<Mat> annotated = mats.stream().map(MatUtils::grey2BGR).collect(Collectors.toList());

    int numMats = mats.size();

    for (int i = 0; i < numMats; i++) {
      LOGGER.info(i + "/" + numMats + " processed");

      Mat mat = mats.get(i);
      Mat anno = annotated.get(i);

      List<KeyPoint> keyPoints = blobDetector.detect(mat);
      LOGGER.info(keyPoints.size() + " key points");
      for (KeyPoint keyPoint : keyPoints) {
        Imgproc.circle(anno, keyPoint.getPoint(), (int) keyPoint.getSigma() * 2, new Scalar(
            ColourBGR.RED), 1);
      }

    }

    new MatViewer(mats, annotated).display();
  }

  public static Lungs getInstance() {
    // Create filter
    BilateralFilter filter =
        new BilateralFilter(getInt(KERNEL_SIZE), getInt(SIGMA_COLOUR), getInt(SIGMA_SPACE));

    // Create ROI extractor
    ROIExtractor extractor =
        new ROIExtractor(getInt(Segmentation.SURE_FG), getInt(Segmentation.SURE_BG));

    // Create blob detector
    int[] neighbourhood =
        new int[] {getInt(NEIGHBOURHOOD_WIDTH), getInt(NEIGHBOURHOOD_HEIGHT),
            getInt(NEIGHBOURHOOD_DEPTH)};
    BlobDetector blobDetector =
        new BlobDetector(neighbourhood, getInt(DOG_THRESH), getInt(GRADIENT_THRESH),
            getInt(NUM_SIGMA));

    // Create lungs instance
    return new Lungs(filter, extractor, blobDetector);
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

    Lungs lungs = getInstance();

    // lungs.gtVsNoduleRoi(stack);
    // lungs.assistance(stack);
    lungs.annotatedSegmented(stack);
    // lungs.roiContours(stack);
    // lungs.blobs(stack);
  }

}
