package optimise;

import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Mat;

import core.Lungs;
import model.CTSlice;
import model.CTStack;
import model.GroundTruth;
import model.ROI;
import util.DataFilter;
import util.MatUtils;
import util.MongoHelper;
import vision.Matcher;

/**
 * Provides access to data and methods to aid the optimisation {@link Lungs}.
 *
 * @author Stuart Clark
 */
public class LungsOptHelper {

  /**
   * The list of {@link Mat}s that will be used to optimise {@link Lungs}. All of these {@link Mat}s
   * should contain nodules.
   */
  private final List<Mat> mats;

  /**
   * {@code groundTruths.get(i)} contains all of the nodules that are found in {@code mats.get(i)}.
   */
  private final List<List<GroundTruth>> groundTruths;

  /**
   * The total number of {@link GroundTruth}s in {@code groundTruths}.
   */
  private int totalGTs;

  /**
   * @param numStacks the number of stacks that should be used.
   */
  public LungsOptHelper(int numStacks) {
    mats = new ArrayList<>();
    groundTruths = new ArrayList<>();
    DataFilter filter = DataFilter.get();

    // Load some stacks
    Datastore ds = MongoHelper.getDataStore();
    Query<CTStack> query = filter.train(ds.createQuery(CTStack.class));
    List<CTStack> stacks = query.asList(new FindOptions().limit(numStacks));

    // For each slice in all the stacks
    for (CTStack stack : stacks) {
      for (CTSlice slice : stack.getSlices()) {

        // Find all the first readings for the slice that contain a nodule. Only one reading
        // is used as we need to know exactingly how many nodules there are in the set of Mats we
        // will use
        List<GroundTruth> gtList =
            filter.singleReading(
                ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
                    .field("imageSopUID").equal(slice.getImageSopUID())).asList();

        // If there are nodules in the slice
        if (!gtList.isEmpty()) {

          // Add Mat for slice into list that will be used in eval(..)
          mats.add(MatUtils.getSliceMat(slice));

          // Add gtLists to list that will be used in eval(..)
          groundTruths.add(gtList);
          totalGTs += gtList.size();
        }

      }
    }
  }

  /**
   * Extract the ROIs for the mats using {@code lungs}.
   *
   * @param lungs
   * @return all the {@link ROI}s extracted using {@code lungs}. Each sublist contains all the ROIs
   *         for the corresponding Mat in {@code mats}
   */
  public List<List<ROI>> extractROIs(Lungs lungs) {
    List<List<ROI>> allROIs = new ArrayList<>();
    for (Mat mat : mats) {
      List<ROI> rois = lungs.extractRois(mat);
      allROIs.add(rois);
    }

    return allROIs;
  }

  protected double noduleInclusion(Lungs lungs) {
    return noduleInclusion(extractROIs(lungs));
  }

  /**
   * @param allROIs a list of lists of {@link ROI}s each sublist should be all of the {@link ROI}s
   *        for the corresponding Mat in {@code this.mats}.
   * @return a {@code double} between 0 and 1 inclusive that indicated how well nodules were
   *         included in the segmented Mat. 1 meaning perfect inclusion 0 meaning no inclusion at
   *         all.
   */
  protected double noduleInclusion(List<List<ROI>> allROIs) {
    double noduleInclusion = 0.0;
    for (int i = 0; i < allROIs.size(); i++) {
      // Get ground truths for segmented mat
      List<GroundTruth> segGts = groundTruths.get(i);
      // Get ROIs for segmented mat
      List<ROI> rois = allROIs.get(i);

      // Find the bestScore for each of the GroundTruths using Matcher
      for (GroundTruth segGt : segGts) {

        double bestScore = 0.0;
        for (ROI roi : rois) {
          double score = Matcher.match(roi, segGt);
          if (score > bestScore) {
            bestScore = score;
          }
        }

        // Add to noduleInclusion
        noduleInclusion += bestScore;
      }

    }

    // Normalise to value between 0 and 1 inclusive and return
    return noduleInclusion / totalGTs;
  }

  public List<Mat> getMats() {
    return mats;
  }

}
