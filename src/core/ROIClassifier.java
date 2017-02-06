package core;

import org.opencv.ml.SVM;

/**
 * Used to to classify ROIs as one of the {@link Class}.
 *
 * @author Stuart Clark
 */
public class ROIClassifier {

  private SVM svm;

  public ROIClassifier() {
    this.svm = SVM.create();
  }

  /**
   * The classes used by the ROIClassifier
   */
  public enum Class {

    NODULE(0.0), NON_NODULE(1.0);

    private double doubleVal;

    Class(double v) {
      this.doubleVal = v;
    }

    public double getDoubleVal() {
      return doubleVal;
    }

  }

}
