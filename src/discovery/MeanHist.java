package discovery;

import org.opencv.core.Mat;

import ml.feature.IntensityHist;
import model.roi.ROI;
import util.LungsException;

/**
 * @author Stuart Clark
 */
public class MeanHist extends IntensityHist {

  private static final int BINS = 256;

  public MeanHist() {
    super(BINS);
  }

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {

  }

  // public MeanHist(double[] bins) {
  // super(bins);
  // }
  //
  // public static void main(String[] args) {
  // Datastore ds = MongoHelper.getDataStore();
  //
  // for (CTSlice slice : DataFilter.get().all(ds.createQuery(CTSlice.class))) {
  // Mat mat = MatUtils.getSliceMat(slice);
  //
  // }
  // }
}
