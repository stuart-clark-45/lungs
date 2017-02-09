package core;

import static model.ROI.Class.NODULE;
import static model.ROI.Class.NON_NODULE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ROI;
import util.MongoHelper;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Used to generate the required arff files to be used with a {@link weka.classifiers.Classifier}.
 *
 * @author Stuart Clark
 */
public class ArffGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArffGenerator.class);
  private static final int LOG_INTERVAL = 5000;
  private static final String TRAIN_FILE = "train.arff";
  private static final String TEST_FILE = "test.arff";

  private Datastore ds;

  public ArffGenerator() {
    this.ds = MongoHelper.getDataStore();
  }

  public void run() throws IOException {
    LOGGER.info("Running Trainer...");

    Query<ROI> rois = ds.createQuery(ROI.class);
    int numROI = (int) rois.count();

    // Create list if attributes and methods to access them
    ArrayList<Attribute> attributes = new ArrayList<>();
    List<Function<ROI, Object>> functions = new ArrayList<>();
    // Add mean intensity
    attributes.add(new Attribute("Mean Intensity"));
    functions.add(ROI::getMeanIntensity);
    // Add class
    attributes.add(new Attribute("Class", Arrays.asList(NODULE.name(), NON_NODULE.name())));
    functions.add(ROI::getClassificaiton);

    int numAttributes = attributes.size();

    // Create training set
    Instances trainingSet = new Instances("Training Set", attributes, numROI);
    trainingSet.setClassIndex(numAttributes - 1);

    LOGGER.info("Creating Instances...");
    int counter = 0;
    for (ROI roi : rois) {

      // Create the Instance
      Instance instance = new DenseInstance(numAttributes);
      for (int i = 0; i < numAttributes; i++) {
        setValue(instance, attributes.get(i), functions.get(i).apply(roi));
      }

      // Add to the training set
      trainingSet.add(instance);

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numROI + " training instances created");
      }
    }

    // Save to arff file
    LOGGER.info("Saving training set to " + TRAIN_FILE);
    ArffSaver saver = new ArffSaver();
    saver.setInstances(trainingSet);
    saver.setFile(new File(TRAIN_FILE));
    saver.writeBatch();

    LOGGER.info("Training complete");
  }

  private static void setValue(Instance instance, Attribute attribute, Object value) {
    if (value instanceof Double) {
      instance.setValue(attribute, (Double) value);
    } else if (value instanceof ROI.Class) {
      instance.setValue(attribute, ((ROI.Class) value).name());
    } else {
      throw new IllegalStateException(value.getClass()
          + " not yet supported by Trainer please add it");
    }
  }

  public static void main(String[] args) throws IOException {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    new ArffGenerator().run();
  }

}
