package discover;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FindOptions;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Misc;
import core.Lungs;
import model.CTSlice;
import model.GroundTruth;
import model.ROI;
import util.ColourBGR;
import util.ConfigHelper;
import util.MatUtils;
import util.MongoHelper;

/**
 * Used to create images that show examples of ROIs that have been matched to nodules with different
 * scores.
 */
public class MatchExamples {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatchExamples.class);
  private static final String IMAGE_DIR = ConfigHelper.getString(Misc.MATCH_EXAMPLES);

  /**
   * @param args args[0] the maximum number of images for each match range to create. arg[1] the
   *        size of each of the match ranges e.g. 0.5 would produce two ranges one from 0-0.5 and
   *        one from 0.5-1.
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    int limit = Integer.parseInt(args[0]);
    double increment = Double.parseDouble(args[1]);

    File imageDir = new File(IMAGE_DIR);
    FileUtils.deleteDirectory(imageDir);
    imageDir.mkdir();

    Datastore ds = MongoHelper.getDataStore();
    FindOptions options = new FindOptions().limit(limit);

    for (double score = 0; score < 1.0; score += increment) {
      // Create a dir to sore images with match score of score
      String scoreDir = IMAGE_DIR + "/" + score + "-" + (score + increment);
      new File(scoreDir).mkdir();

      // Find ROIs with in match range
      List<ROI> rois =
          ds.createQuery(ROI.class).field("matchScore").greaterThan(score).field("matchScore")
              .lessThanOrEq(score + increment).asList(options);
      LOGGER.info(rois.size() + " ROIs found with matchScore in range " + score + "-" + score
          + increment);

      // Create a mat with the ROI painted green and the outline of the GroundTruth painted red
      for (int i = 0; i < rois.size(); i++) {
        ROI roi = rois.get(i);
        GroundTruth gt = roi.getGroundTruth();
        CTSlice slice =
            ds.createQuery(CTSlice.class).field("imageSopUID").equal(gt.getImageSopUID()).get();
        Mat mat = MatUtils.grey2BGR(MatUtils.getSliceMat(slice));

        // Paint the ROI
        Lungs.paintROI(mat, roi, ColourBGR.GREEN);

        // Paint the GroundTruth
        for (Point point : gt.getEdgePoints()) {
          MatUtils.put(mat, point, ColourBGR.RED);
        }

        // Write mat to file
        Imgcodecs.imwrite(scoreDir + "/" + i + ".bmp", mat);
      }

    }

  }

}
