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
import model.ReadingROI;
import util.ColourBGR;
import util.ConfigHelper;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;

public class Lungs {

  private static final Logger LOGGER = LoggerFactory.getLogger(Lungs.class);
  private static final int CROSS_SIZE = 5;
  private static final int CROSS_THICKNESS = 1;

  private Datastore ds;

  public Lungs() {
    ds = MongoHelper.getDataStore();
  }

  private void groundTruth() {
    CTStack stack = ds.createQuery(CTStack.class).get();
    // CTStack ctStack = ds.createQuery(CTStack.class).field("model").equal("LightSpeed16").get(new
    // FindOptions().skip(1));

    List<Mat> greyMats = new ArrayList<>(stack.size());
    List<Mat> annotatedMats = new ArrayList<>(stack.size());
    for (CTSlice slice : stack.getSlices()) {
      Mat grey = getSliceMat(slice);
      greyMats.add(grey);

      Mat annotated = MatUtils.grey2RGB(grey);
      annotate(annotated, slice);
      annotatedMats.add(annotated);
    }

    new MatViewer(greyMats, annotatedMats).display();
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

    if (type == BIG_NODULE && ConfigHelper.getBoolean(Annotation.BIG_NODULE)) {
      for (Point point : roi.getEdgePoints()) {
        Imgproc.drawMarker(rgb, point, new Scalar(ColourBGR.RED), MARKER_SQUARE, 1, 1, LINE_4);
      }

    } else if (type == SMALL_NODULE && ConfigHelper.getBoolean(Annotation.SMALL_NODULE)) {
      Imgproc.drawMarker(rgb, roi.getCentroid(), new Scalar(ColourBGR.RED), MARKER_TILTED_CROSS,
          CROSS_SIZE, CROSS_THICKNESS, LINE_4);

    } else if (type == NON_NODULE && ConfigHelper.getBoolean(Annotation.NON_NODULE)) {
      Imgproc.drawMarker(rgb, roi.getCentroid(), new Scalar(ColourBGR.GREEN), MARKER_TILTED_CROSS,
          CROSS_SIZE, 1, LINE_4);
    }

  }

  private void segment() {
    CTStack stack = ds.createQuery(CTStack.class).get();
    int numSlices = stack.size();
    List<Mat> original = getStackMats(stack);

    List<Mat> annotated = new ArrayList<>(numSlices);
    List<CTSlice> slices = stack.getSlices();

    for (int i = 0; i < numSlices; i++) {
      Mat orig = original.get(i);

      Mat filtered = MatUtils.similarMat(orig);
      double sigma = 20d;
      Imgproc.bilateralFilter(orig, filtered, -1, sigma, sigma);

      Mat seg = MatUtils.similarMat(filtered);
      Imgproc.threshold(orig, seg, 60, 255, THRESH_BINARY);

      Mat rgb = MatUtils.grey2RGB(seg);
      annotate(rgb, slices.get(i));
      annotated.add(rgb);

      LOGGER.info(i + 1 + "/" + numSlices + " processed");
    }

    new MatViewer(original, annotated).display();
  }

  /**
   * @param stack
   * @return List of grey-scale {@link Mat} for the given stack.
   */
  private List<Mat> getStackMats(CTStack stack) {
    return stack.getSlices().parallelStream().map(this::getSliceMat).collect(Collectors.toList());
  }

  /**
   * @param slice
   * @return a grey-scale {@link Mat} for the given slice.
   */
  private Mat getSliceMat(CTSlice slice) {
    DICOM dicom = new DICOM();
    dicom.open(slice.getFilePath());
    return MatUtils.fromDICOM(dicom);
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    Lungs lungs = new Lungs();
    // lungs.groundTruth();
    lungs.segment();
  }

}
