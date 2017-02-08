package core;

import static config.Segmentation.Opening;
import static config.Segmentation.Filter.SIGMA_COLOUR;
import static config.Segmentation.Filter.SIGMA_SPACE;
import static config.Segmentation.Filter.SIZE;
import static config.Segmentation.Opening.HEIGHT;
import static config.Segmentation.Opening.WIDTH;
import static config.Segmentation.Threshold.C;
import static config.Segmentation.Threshold.METHOD;
import static model.GroundTruth.Type.BIG_NODULE;
import static model.GroundTruth.Type.NON_NODULE;
import static model.GroundTruth.Type.SMALL_NODULE;
import static org.opencv.imgproc.Imgproc.LINE_4;
import static org.opencv.imgproc.Imgproc.MARKER_SQUARE;
import static org.opencv.imgproc.Imgproc.MARKER_TILTED_CROSS;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static util.ConfigHelper.getInt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Annotation;
import config.Segmentation.Threshold;
import ij.plugin.DICOM;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.ColourBGR;
import util.ConfigHelper;
import util.LungsException;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;
import vision.ROIExtractor;

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
  private int sigmaColour;
  private int sigmaSpace;
  private int filterSize;
  private int thresholdMethod;
  private int thresholdSize;
  private int thresholdC;
  private int openingWidth;
  private int openingHeight;
  private int openingKernel;

  public Lungs() {
    this(getInt(SIGMA_COLOUR), getInt(SIGMA_SPACE), getInt(SIZE), getInt(METHOD),
        getInt(Threshold.SIZE), getInt(C), getInt(WIDTH), getInt(HEIGHT), getInt(Opening.KERNEL));
  }

  public Lungs(int sigmaColour, int sigmaSpace, int filterSize, int thresholdMethod,
      int thresholdSize, int thresholdC, int openingWidth, int openingHeight, int openingKernel) {
    this.thresholdMethod = thresholdMethod;
    this.thresholdSize = thresholdSize;
    this.thresholdC = thresholdC;
    this.ds = MongoHelper.getDataStore();
    this.sigmaColour = sigmaColour;
    this.sigmaSpace = sigmaSpace;
    this.filterSize = filterSize;
    this.openingWidth = openingWidth;
    this.openingHeight = openingHeight;
    this.openingKernel = openingKernel;
  }

  /**
   * @param slices the slices to annotations.
   * @param original the corresponding {@link Mat}s for the {@code slices}.
   * @return the {@code original} {@link Mat}s with the ground truth annotated upon then.
   */
  private List<Mat> groundTruth(List<CTSlice> slices, List<Mat> original) {
    List<Mat> annotated = new ArrayList<>(slices.size());

    for (int i = 0; i < slices.size(); i++) {
      CTSlice slice = slices.get(i);
      Mat mat = original.get(i);
      Mat rgb = MatUtils.grey2RGB(mat);
      annotate(rgb, slice);
      annotated.add(rgb);
    }

    return annotated;
  }

  /**
   * Annotate {@code rgb} with the appropriate annotations for the {@code slice} if they are allowed
   * by the system configuration (see ./conf/application.conf).
   *
   * @param rgb
   * @param slice
   */
  private void annotate(Mat rgb, CTSlice slice) {
    List<GroundTruth> groundTruths =
        ds.createQuery(GroundTruth.class).field("imageSopUID").equal(slice.getImageSopUID())
            .asList();

    for (GroundTruth gt : groundTruths) {
      annotate(rgb, gt);
    }

  }

  /**
   * Annotate {@code rgb} with the appropriate annotations for the {@code gt} if they are allowed by
   * the system configuration (see ./conf/application.conf).
   * 
   * @param rgb
   * @param gt
   */
  private void annotate(Mat rgb, GroundTruth gt) {
    GroundTruth.Type type = gt.getType();

    // Annotate big nodules
    if (type == BIG_NODULE && ConfigHelper.getBoolean(Annotation.BIG_NODULE)) {
      for (Point point : gt.getEdgePoints()) {
        Imgproc.drawMarker(rgb, point, new Scalar(ColourBGR.RED), MARKER_SQUARE, 1, 1, LINE_4);
      }

      // Annotate small nodules
    } else if (type == SMALL_NODULE && ConfigHelper.getBoolean(Annotation.SMALL_NODULE)) {
      Imgproc.drawMarker(rgb, gt.getCentroid(), new Scalar(ColourBGR.RED), MARKER_TILTED_CROSS,
          CROSS_SIZE, CROSS_THICKNESS, LINE_4);

      // Annotate non nodules
    } else if (type == NON_NODULE && ConfigHelper.getBoolean(Annotation.NON_NODULE)) {
      Imgproc.drawMarker(rgb, gt.getCentroid(), new Scalar(ColourBGR.GREEN), MARKER_TILTED_CROSS,
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
      Imgproc.bilateralFilter(orig, filtered, filterSize, sigmaColour, sigmaSpace);

      // Threshold it
      Mat seg = MatUtils.similarMat(filtered);
      Imgproc.adaptiveThreshold(orig, seg, FOREGROUND, thresholdMethod, THRESH_BINARY,
          thresholdSize, thresholdC);

      // Apply opening
      Mat opened = MatUtils.similarMat(seg);
      Imgproc.morphologyEx(seg, opened, Imgproc.MORPH_OPEN,
          Imgproc.getStructuringElement(openingKernel, new Size(openingWidth, openingHeight)));

      segmented.add(opened);
    }

    return segmented;
  }

  /**
   * @param slices
   * @param segmented
   * @return the {@link ROI}s for the segmented images
   * @throws LungsException
   */
  public List<ROI> roiExtraction(List<CTSlice> slices, List<Mat> segmented) throws LungsException {
    int numMat = segmented.size();
    ROIExtractor extractor = new ROIExtractor(FOREGROUND);
    List<ROI> rois = new ArrayList<>();

    // Iterate over mats
    for (int i = 0; i < numMat; i++) {
      Mat mat = segmented.get(i);
      String imageSopUID = slices.get(i).getImageSopUID();

      // Set the imageSopUID for each of the ROIs and save them
      for (ROI roi : extractor.extract(mat)) {
        roi.setImageSopUID(imageSopUID);
        rois.add(roi);
      }

    }

    return rois;
  }

  /**
   * @param stack
   * @return List of grey-scale {@link Mat} for the given stack.
   */
  public static List<Mat> getStackMats(CTStack stack) {
    return stack.getSlices().parallelStream().map(Lungs::getSliceMat).collect(Collectors.toList());
  }

  /**
   * @param slice
   * @return a grey-scale {@link Mat} for the given slice.
   */
  public static Mat getSliceMat(CTSlice slice) {
    DICOM dicom = new DICOM();
    dicom.open(slice.getFilePath());
    return MatUtils.fromDICOM(dicom);
  }

  /**
   * Should be run with the following VM args
   * -Djava.library.path=/usr/local/opt/opencv3/share/OpenCV/java -Xss515m -Xmx6g
   *
   * @param args
   * @throws LungsException
   */
  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Load the images
    CTStack stack = MongoHelper.getDataStore().createQuery(CTStack.class).get();
    List<Mat> original = getStackMats(stack);

    Lungs lungs = new Lungs();

    List<Mat> segmented = lungs.segment(original);

    List<Mat> annotated = lungs.groundTruth(stack.getSlices(), segmented);

    new MatViewer(segmented, annotated).display();
  }

}
