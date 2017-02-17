package ml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.roi.ROI;
import util.DataFilter;
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

  private final DataFilter filter;
  private final Datastore ds;
  private final InstancesBuilder builder;

  public ArffGenerator() {
    ds = MongoHelper.getDataStore();
    builder = new InstancesBuilder(true);
    filter = DataFilter.get();
  }

  public void run() throws Exception {
    LOGGER.info("Running ArffGenerator...");

    // Create training set
    Instances trainingSet = createInstances("Training Set", filter::train, true);

    // Create testing set
    Instances testingSet = createInstances("Testing Set", filter::test, false);

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
   * @param filterMethod the method of {@link DataFilter} that should be used to filter query
   *        results. i.e this should be either {@code filter::train} or {@code filter::test}.
   * @param limitOn true if the number of nodules and non-nodules should be the same in the
   *        {@link Instances} returned, false is all the non-nodules available should be used.
   * @return
   */
  private Instances createInstances(String name, Function<Query<ROI>, Query<ROI>> filterMethod,
      boolean limitOn) {
    // Find all the nodules
    Query<ROI> nodules =
        filterMethod.apply(ds.createQuery(ROI.class).field(CLASS).equal(ROI.Class.NODULE));
    int numNodules = (int) nodules.count();

    // Set the limit on non-nodules if required
    FindOptions options = new FindOptions();
    if (limitOn) {
      options.limit(numNodules);
    }

    // Find the required number of non-nodules
    @SuppressWarnings("ConstantConditions")
    List<ROI> nonNodule =
        filterMethod.apply(ds.createQuery(ROI.class).field(CLASS).equal(ROI.Class.NON_NODULE))
            .asList(options);

    // Logging counts for each class
    int numNonNodule = nonNodule.size();
    LOGGER.info(name + " will have:\n" + numNodules + " NODULES\n" + numNonNodule + " NON_NODULES");

    // Create the set
    Instances set = builder.createSet(name, numNodules + numNonNodule);
    builder.addInstances(set, nodules);
    builder.addInstances(set, nonNodule);

    return set;
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
