package core;

import static org.opencv.ml.Ml.ROW_SAMPLE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.TrainData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ROI;
import util.MongoHelper;

/**
 * Used to train a classier to identify nodules. // TODO add diagnosis
 *
 * @author Stuart Clark
 */
public class Trainer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Trainer.class);
  private static final int LOG_INTERVAL = 5000;

  private Datastore ds;

  public Trainer() {
    this.ds = MongoHelper.getDataStore();
  }

  public void run() {
    LOGGER.info("Running Trainer...");

    Query<ROI> rois = ds.createQuery(ROI.class);
    int numROI = (int) rois.count();

    // Create a list of the functions to call to obtain values for features
    List<Function<ROI, Double>> features = new ArrayList<>();
    features.add(Trainer::meanIntensity);

    Mat samples = new Mat(numROI, features.size(), CvType.CV_32SC1);
    Mat labels = new Mat(numROI, 1, CvType.CV_32SC1);

    LOGGER.info("Creating samples...");
    Iterator<ROI> iterator = rois.iterator();
    for (int row = 0; row < numROI; row++) {

      // Get the next ROI
      ROI roi = iterator.next();

      // Set it's label
      labels.put(row, 1, roi.getClassificaiton().getDoubleVal());

      // Fill it's row in the samples Mat
      for (int col = 0; col < features.size(); col++) {
        samples.put(row, col, features.get(col).apply(roi));
      }

      if (row % LOG_INTERVAL == 0) {
        LOGGER.info(row + "/" + numROI + " samples created");
      }
    }

    // Train the classifier and save it to a file
    LOGGER.info("Training classifier");
    TrainData trainData = TrainData.create(samples, ROW_SAMPLE, labels);
    ROIClassifier classifier = new ROIClassifier();
    classifier.train(trainData);
    LOGGER.info("Writing classifier to file");
    classifier.save();

    LOGGER.info("Training complete");
  }

  private static Double meanIntensity(ROI roi) {
    return roi.getMeanIntensity();
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    new Trainer().run();
  }

}
