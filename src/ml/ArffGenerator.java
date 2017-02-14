package ml;

import static util.DataFilter.TEST_INSTANCE;
import static util.DataFilter.TRAIN_INSTANCE;

import java.io.File;
import java.io.IOException;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.roi.ROI;
import util.MongoHelper;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Used to generate the required arff files to be used with a {@link weka.classifiers.Classifier}.
 *
 * @author Stuart Clark
 */
public class ArffGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArffGenerator.class);
  public static final String TRAIN_FILE = "train.arff";
  public static final String TEST_FILE = "test.arff";
  public static final String ALL_FILE = "all.arff";

  private Datastore ds;
  private InstancesBuilder builder;

  public ArffGenerator() {
    ds = MongoHelper.getDataStore();
    builder = new InstancesBuilder(true);
  }

  public void run() throws IOException {
    LOGGER.info("Running ArffGenerator...");

    // Create training set
    Query<ROI> trainQuery =
        ds.createQuery(ROI.class).field("seriesInstanceUID").equal(TRAIN_INSTANCE);
    Instances trainingSet = builder.instances("Training Set", trainQuery);

    // Create testing set
    Query<ROI> testQuery =
        ds.createQuery(ROI.class).field("seriesInstanceUID").equal(TEST_INSTANCE);
    Instances testingSet = builder.instances("Testing Set", testQuery);

    // Combine testing and training sets
    Instances all =
        new Instances("Combined Set", builder.getAttributes(), trainingSet.size()
            + testingSet.size());
    all.addAll(trainingSet);
    all.addAll(testingSet);

    // Create arff files
    save(trainingSet, TRAIN_FILE);
    save(testingSet, TEST_FILE);
    save(all, ALL_FILE);

    LOGGER.info("ArffGenerator finished running");
  }

  /**
   * Save the {@code instances} to a file with name {@code file}.
   *
   * @param instances
   * @param file
   */
  private void save(Instances instances, String file) {
    try {
      LOGGER.info("Saving instances to " + file);
      ArffSaver saver = new ArffSaver();
      saver.setInstances(instances);
      saver.setFile(new File(file));
      saver.writeBatch();
    } catch (IOException e) {
      LOGGER.error("Failed to write to " + file, e);
    }
  }

  public static void main(String[] args) throws IOException {
    new ArffGenerator().run();
  }

}
