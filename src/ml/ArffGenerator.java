package ml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ROI;
import util.BatchIterator;
import util.LimitedIterator;
import util.MongoHelper;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Used to generate the required arff files to be used with a {@link weka.classifiers.Classifier}.
 *
 * @author Stuart Clark
 */
public class ArffGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArffGenerator.class);

  /**
   * The number of {@link Instances} to create before saving to a file.
   */
  private static final int BATCH_SIZE = 1000000;

  public static final String TRAIN_FILE = "train.arff";
  public static final String TEST_FILE = "test.arff";
  public static final String ALL_FILE = "all.arff";
  private static final String CLASS = "classification";
  private static final String SET = "set";

  private final Datastore ds;
  private final InstancesBuilder builder;

  /**
   * Used to save {@link Instances} to the {@code TRAIN_FILE}.
   */
  private final ArffSaver trainSaver;

  /**
   * Used to save {@link Instances} to the {@code TEST_FILE}.
   */
  private final ArffSaver testSaver;

  /**
   * Used to save {@link Instances} to the {@code ALL_FILE}.
   */
  private final ArffSaver allSaver;

  public ArffGenerator() throws IOException {
    ds = MongoHelper.getDataStore();
    builder = new InstancesBuilder(true);

    trainSaver = new ArffSaver();
    trainSaver.setFile(new File(TRAIN_FILE));

    testSaver = new ArffSaver();
    testSaver.setFile(new File(TEST_FILE));

    allSaver = new ArffSaver();
    allSaver.setFile(new File(ALL_FILE));
  }

  public void run() throws Exception {
    LOGGER.info("Running ArffGenerator...");

    // Create the arff files
    createTestFile();
    createTrainFile();

    // Test the classifier
    LOGGER.info("Testing the classifier...");

    // Build classifier
    Classifier cls = new J48();
    cls.buildClassifier(trainingSet);
    // Evaluate classifier and print some statistics
    Evaluation eval = new Evaluation(trainingSet);
    eval.evaluateModel(cls, testingSet);
    LOGGER.info(eval.toSummaryString("\nResults\n======\n", false));
    LOGGER.info(eval.toClassDetailsString("\n=== Detailed Accuracy By Class ===\n"));
    LOGGER.info(eval.toMatrixString("\n=== Confusion Matrix ===\n"));
    LOGGER.info("ArffGenerator finished running");
  }

  private void createTrainFile() throws IOException {
    createFile(trainSaver, "Training Set", ROI.Set.TRAIN, true);
  }

  private void createTestFile() throws IOException {
    createFile(testSaver, "Testing Set", ROI.Set.TEST, false);
  }

  /**
   * Used to create arff files for the training and testing sets. (Also creates an arff file that
   * contains all the {@link Instances} from both sets).
   *
   * @param saver the {@link ArffSaver} to use when creating the file. Should be either
   *        {@code this.trainSaver} or {@code this.testSaver}.
   * @param name the name to give the {@link Instances} saved to the file.
   * @param set the set you would like to create i.e {@link ROI.Set#TRAIN} or {@link ROI.Set#TEST}.
   * @param limitOn true if the number of nodules and non-nodules should be the same in the
   *        {@link Instances} returned, false is all the non-nodules available should be used.
   * @return
   */
  private void createFile(ArffSaver saver, String name, ROI.Set set, boolean limitOn)
      throws IOException {
    // Find all the nodules
    LOGGER.info("Finding all NODULE for " + name);
    Query<ROI> query =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NODULE);
    int numNodules = (int) query.count();

    // Write all the nodules to the arff file
    Instances noduleInstances = builder.createSet(name, numNodules);
    BatchIterator<ROI> noduleIterator = new BatchIterator<>(query.iterator(), BATCH_SIZE);
    while (noduleIterator.hasNext()) {
      builder.addInstances(noduleInstances, noduleIterator, BATCH_SIZE);
      saver.writeBatch();
      noduleIterator.nextBatch();
    }

    // Find all the non nodules
    LOGGER.info("Finding all NON_NODULE for " + name);
    query =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NON_NODULE);

    Iterator<ROI> nonNodule;
    if (limitOn) {
      nonNodule = new LimitedIterator<>(query.iterator(), numNodules);
    } else {
      nonNodule = new BatchIterator<>(query.iterator(), BATCH_SIZE);
    }

    // Get the iterator and the number of non nodules used
    Iterator<ROI> nonNodule;
    int numNonNodule;
    if (limitOn) {
      nonNodule = new BatchIterator<>(query.iterator(), numNodules);
      numNonNodule = numNodules;
    } else {
      nonNodule = query.iterator();
      numNonNodule = (int) query.count();
    }

    // Logging counts for each class
    LOGGER.info(name + " will have:\n" + numNodules + " NODULES\n" + numNonNodule + " NON_NODULES");

    // Create the instances


    return instances;
  }

  private void batchWrite(ArffSaver saver, String name, String size, Query<ROI> query) {
    Instances noduleInstances = builder.createSet(name, size);
    BatchIterator<ROI> noduleIterator = new BatchIterator<>(query.iterator(), BATCH_SIZE);
    while (noduleIterator.hasNext()) {
      builder.addInstances(noduleInstances, noduleIterator, BATCH_SIZE);
      saver.writeBatch();
      noduleIterator.nextBatch();
    }
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
      saver.setFile(new File(file));
      saver.setInstances(instances);
      saver.writeBatch();
    } catch (IOException e) {
      LOGGER.error("Failed to write to " + file, e);
    }
  }

  public static void main(String[] args) throws Exception {
    new ArffGenerator().run();
  }

}
