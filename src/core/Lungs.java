package core;

import static model.ReadingROI.Type.BIG_NODULE;
import static model.ReadingROI.Type.NON_NODULE;
import static model.ReadingROI.Type.SMALL_NODULE;
import static org.opencv.imgproc.Imgproc.LINE_4;
import static org.opencv.imgproc.Imgproc.MARKER_SQUARE;
import static org.opencv.imgproc.Imgproc.MARKER_TILTED_CROSS;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Annotation;
import ij.plugin.DICOM;
import model.CTSlice;
import model.CTStack;
import model.ROI;
import model.ReadingROI;
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
  private static final int FOREGROUND = 255;

  private Datastore ds;

  public Lungs() {
    ds = MongoHelper.getDataStore();
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
    List<ReadingROI> rois =
        ds.createQuery(ReadingROI.class).field("imageSopUID").equal(slice.getImageSopUID())
            .asList();

    for (ReadingROI roi : rois) {
      annotate(rgb, roi);
    }

  }

  /**
   * Annotate {@code rgb} with the appropriate annotations for the {@code roi} if they are allowed
   * by the system configuration (see ./conf/application.conf).
   * 
   * @param rgb
   * @param roi
   */
  private void annotate(Mat rgb, ReadingROI roi) {
    ReadingROI.Type type = roi.getType();

    // Annotate big nodules
    if (type == BIG_NODULE && ConfigHelper.getBoolean(Annotation.BIG_NODULE)) {
      for (Point point : roi.getEdgePoints()) {
        Imgproc.drawMarker(rgb, point, new Scalar(ColourBGR.RED), MARKER_SQUARE, 1, 1, LINE_4);
      }

      // Annotate small nodules
    } else if (type == SMALL_NODULE && ConfigHelper.getBoolean(Annotation.SMALL_NODULE)) {
      Imgproc.drawMarker(rgb, roi.getCentroid(), new Scalar(ColourBGR.RED), MARKER_TILTED_CROSS,
          CROSS_SIZE, CROSS_THICKNESS, LINE_4);

      // Annotate non nodules
    } else if (type == NON_NODULE && ConfigHelper.getBoolean(Annotation.NON_NODULE)) {
      Imgproc.drawMarker(rgb, roi.getCentroid(), new Scalar(ColourBGR.GREEN), MARKER_TILTED_CROSS,
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

    for (int i = 0; i < numMat; i++) {
      Mat orig = original.get(i);

      Mat filtered = MatUtils.similarMat(orig);
      double sigma = 20d;
      Imgproc.bilateralFilter(orig, filtered, -1, sigma, sigma);

      Mat seg = MatUtils.similarMat(filtered);
      Imgproc.threshold(orig, seg, 60, FOREGROUND, THRESH_BINARY);
      segmented.add(seg);

      LOGGER.info(i + 1 + "/" + numMat + " segmented");
    }

    return segmented;
  }

  /**
   * Extract the {@link ROI}s for the segmented images and save them to the database.
   * 
   * @param slices
   * @param segmented
   * @throws LungsException
   */
  public void roiExtraction(List<CTSlice> slices, List<Mat> segmented) throws LungsException {
    int numMat = segmented.size();
    ROIExtractor extractor = new ROIExtractor(FOREGROUND);

    // Iterate over mats
    for (int i = 0; i < numMat; i++) {
      Mat mat = segmented.get(i);
      String imageSopUID = slices.get(i).getImageSopUID();

      // Set the imageSopUID for each of the ROIs and save them
      for (ROI roi : extractor.extract(mat)) {
        roi.setImageSopUID(imageSopUID);
        ds.save(roi);
      }

      LOGGER.info(i + 1 + "/" + numMat + " had ROIs extracted");
    }
  }

  /**
   * @param stack
   * @return List of grey-scale {@link Mat} for the given stack.
   */
  private static List<Mat> getStackMats(CTStack stack) {
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

  public static void main(String[] args) throws LungsException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // Load the images
    CTStack stack = MongoHelper.getDataStore().createQuery(CTStack.class).get();
    List<Mat> original = getStackMats(stack);

    Lungs lungs = new Lungs();

    List<Mat> segmented = lungs.segment(original);

    lungs.roiExtraction(stack.getSlices(), segmented);

    List<Mat> annotated = lungs.groundTruth(stack.getSlices(), segmented);

    new MatViewer(segmented, annotated).display();
  }

}
