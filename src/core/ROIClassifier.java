package core;

import org.opencv.ml.SVM;
import org.opencv.ml.TrainData;

/**
 * Used to to classify ROIs as one of the {@link Class}.
 *
 * @author Stuart Clark
 */
public class ROIClassifier {

  private static final String FILE_NAME = "roi-classifier.xml";

  private SVM svm;

  public void train(TrainData data) {
    svm = SVM.create();
    svm.train(data);
  }

  public void save() {
    svm.save(FILE_NAME);
  }

  public void load() {
    svm = SVM.load(FILE_NAME);
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
