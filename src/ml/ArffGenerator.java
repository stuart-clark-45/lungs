package ml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.roi.ROI;
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
  public static final String TRAIN_FILE = "train.arff";
  public static final String TEST_FILE = "test.arff";
  public static final String ALL_FILE = "all.arff";
  private static final String CLASS = "classification";
  private static final String SET = "set";

  private final Datastore ds;
  private final InstancesBuilder builder;

  public ArffGenerator() {
    ds = MongoHelper.getDataStore();
    builder = new InstancesBuilder(true);
  }

  public void run() throws Exception {
    LOGGER.info("Running ArffGenerator...");

    // Create training set
    Instances trainingSet = createInstances("Training Set", ROI.Set.TRAIN, true);

    // Create testing set
    Instances testingSet = createInstances("Testing Set", ROI.Set.TEST, false);

    // Combine testing and training sets
    Instances all = builder.createSet("Combined Set", trainingSet.size() + testingSet.size());
    all.addAll(trainingSet);
    all.addAll(testingSet);

    // Create arff files
    save(trainingSet, TRAIN_FILE);
    save(testingSet, TEST_FILE);
    save(all, ALL_FILE);

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

  /**
   * Used to create training and testing sets.
   *
   * @param name the name to give the {@link Instances} returned.
   * @param set the set you would like to create i.e {@link ROI.Set#TRAIN} or {@link ROI.Set#TEST}.
   * @param limitOn true if the number of nodules and non-nodules should be the same in the
   *        {@link Instances} returned, false is all the non-nodules available should be used.
   * @return
   */
  private Instances createInstances(String name, ROI.Set set, boolean limitOn) {
    // Find all the nodules
    Query<ROI> nodules =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NODULE);
    int numNodules = (int) nodules.count();

    // Find all the non nodules
    Query<ROI> query =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NON_NODULE);

    // Get the iterator and the number of non nodules used
    Iterator<ROI> nonNodule;
    int numNonNodule = 0;
    if (limitOn) {
      nonNodule = new LimitedIterator<>(query.iterator(), numNodules);
      numNonNodule = numNodules;
    } else {
      nonNodule = query.iterator();
      numNodules = (int) query.count();
    }

    // Logging counts for each class
    LOGGER.info(name + " will have:\n" + numNodules + " NODULES\n" + numNonNodule + " NON_NODULES");

    // Create the instances
    Instances instances = builder.createSet(name, numNodules + numNonNodule);
    builder.addInstances(instances, nodules);
    builder.addInstances(instances, nonNodule, numNonNodule);

    return instances;
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

  public static void main(String[] args) throws Exception {
    new ArffGenerator().run();
  }

}
