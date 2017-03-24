package ml.feature;

import static model.Histogram.POS_VALS_8BIT;

import org.opencv.core.Mat;

import model.Histogram;
import model.ROI;
import util.LungsException;

/**
 * Creates histograms for the {@link ROI} with different numbers of bins and updates the {@link ROI}
 * .
 *
 * @author Stuart Clark
 */
public class AllHists implements Feature {

  public static final int COARSE = 16;
  public static final int MID = 64;
  public static final int FINE = 128;

  @Override
  public void compute(ROI roi, Mat mat) throws LungsException {
    Histogram fine = new Histogram(FINE, POS_VALS_8BIT);
    fine.add(roi.getRegion(), mat);
    fine.computeBins();
    fine.toFrequencies();
    roi.setFineHist(fine);

    Histogram mid = new Histogram(MID, fine);
    mid.computeBins();
    mid.toFrequencies();
    roi.setMedHist(mid);

    Histogram coarse = new Histogram(COARSE, fine);
    coarse.computeBins();
    coarse.toFrequencies();
    roi.setCoarseHist(coarse);
  }

}
