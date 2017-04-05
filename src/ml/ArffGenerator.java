package ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.collections4.IterableUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.ROI;
import util.LimitedIterable;
import util.MongoHelper;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.converters.ArffSaver;
import weka.core.converters.Saver;

/**
 * Used to generate the required arff files to be used with a {@link weka.classifiers.Classifier}.
 *
 * @author Stuart Clark
 */
public class ArffGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArffGenerator.class);

  private static final int LOG_INTERVAL = 5000;

  public static final String TRAIN_FILE = "train.arff";
  public static final String TEST_FILE = "test.arff";
  private static final String CLASS = "classification";
  private static final String SET = "set";

  private final Datastore ds;
  private final InstancesBuilder builder;

  public ArffGenerator() throws IOException {
    ds = MongoHelper.getDataStore();
    builder = new InstancesBuilder(true);
  }

  public void run() throws Exception {
    LOGGER.info("Running ArffGenerator...");

    // Create the arff files
    createTrainFile();
    createTestFile();

    // Test the classifier
    LOGGER.info("Testing the classifier...");

    // Load trainingData
    BufferedReader reader = new BufferedReader(new FileReader(TRAIN_FILE));
    ArffReader arff = new ArffReader(reader, 0);
    Instances trainData = arff.getStructure();
    trainData.setClassIndex(trainData.numAttributes() - 1);

    // Incrementally build classifier
    Classifier classifier = Lungs.newClassifier();
    Instance instance;
    while ((instance = arff.readInstance(trainData)) != null) {
      ((UpdateableClassifier) classifier).updateClassifier(instance);
    }

    // Save classifier model
    Lungs.writeClassifier(classifier);

    // Load testData
    reader = new BufferedReader(new FileReader(TEST_FILE));
    arff = new ArffReader(reader, 0);
    Instances testData = arff.getStructure();
    testData.setClassIndex(testData.numAttributes() - 1);

    // Incrementally classify each of the instances in testData
    Evaluation eval = new Evaluation(testData);
    Instance inst;
    while ((inst = arff.readInstance(testData)) != null) {
      eval.evaluationForSingleInstance(classifier.distributionForInstance(inst), inst, true);
    }

    // Evaluate classifier and print some statistics
    eval.evaluateModel(classifier, testData);
    LOGGER.info(eval.toSummaryString("\nResults\n======\n", false));
    LOGGER.info(eval.toClassDetailsString("\n=== Detailed Accuracy By Class ===\n"));
    LOGGER.info(eval.toMatrixString("\n=== Confusion Matrix ===\n"));
    LOGGER.info("ArffGenerator finished running");
  }

  private void createTrainFile() throws IOException {
    createFile("Training Set", TRAIN_FILE, ROI.Set.TRAIN, true);
  }

  private void createTestFile() throws IOException {
    createFile("Testing Set", TEST_FILE, ROI.Set.TEST, false);
  }

  /**
   * Used to create arff files for the training and testing sets. (Also creates an arff file that
   * contains all the {@link Instances} from both sets).
   *
   * @param file the name of the arff file to create
   * @param name the name to give the {@link Instances} saved to the file.
   * @param set the set you would like to create i.e {@link ROI.Set#TRAIN} or {@link ROI.Set#TEST}.
   * @param oversample true if nodules should be oversampled so that there are the same number of
   *        nodules and non-nodules
   */
  private void createFile(String name, String file, ROI.Set set, boolean oversample)
      throws IOException {
    LOGGER.info("Creating " + name + "...");

    // Find all the non nodules
    LOGGER.info("Finding all NON_NODULE for " + name);
    Query<ROI> nonNodules =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NON_NODULE);
    long numNonNodule = nonNodules.count();

    // Find all the nodules
    LOGGER.info("Finding all NODULE for " + name);
    Query<ROI> noduleQuery =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NODULE);
    long numNodule = noduleQuery.count();

    // Oversample nodules if required
    Iterable<ROI> nodules;
    if (oversample) {
      nodules = oversample(noduleQuery, numNodule, numNonNodule);
      numNodule = numNonNodule;
    } else {
      nodules = noduleQuery;
    }

    // Log the size of the classes
    LOGGER.info(name + " will have:\n" + numNodule + " NODULES\n" + numNonNodule + " NON_NODULES");

    // Create the ArffSaver
    ArffSaver saver = new ArffSaver();
    saver.setFile(new File(file));
    saver.setRetrieval(Saver.INCREMENTAL);
    Instances instances = builder.createSet(name, (int) (numNodule + numNonNodule));
    saver.setStructure(instances);

    // Write all the nodules to the arff file
    int counter = 0;
    for (ROI roi : nodules) {
      Instance instance = builder.createInstance(roi);
      instance.setDataset(instances);
      saver.writeIncremental(instance);

      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numNonNodule + " NODULES have been added to the arff file");
      }
    }

    // Write all the non-nodules to the arff file
    counter = 0;
    for (ROI roi : nonNodules) {
      Instance instance = builder.createInstance(roi);
      instance.setDataset(instances);
      saver.writeIncremental(instance);

      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numNonNodule + " NON_NODULES have been added to the arff file");
      }
    }

    // Flushes the instances to the file
    saver.writeIncremental(null);
  }

  /**
   * @param noduleQuery
   * @param numNodule
   * @param numNonNodule
   * @return an iterable with {@code numNonNodule} elements, produced by repeating the ROIs in
   *         nodule query.
   */
  private Iterable<ROI> oversample(Query<ROI> noduleQuery, long numNodule, long numNonNodule) {
    // Calculate how many times the nodules query need to be repeated
    long numRequired = numNonNodule - numNodule;
    long numIterables = numRequired / numNodule;

    // Add all full queries to nodules
    Iterable<ROI> nodules = noduleQuery.cloneQuery();
    for (long i = 1; i < numIterables; i++) {
      nodules = IterableUtils.chainedIterable(nodules, noduleQuery.cloneQuery());
    }

    // Add a limited iterable that contains the remainder required to balance the sets
    numRequired = numRequired - (numIterables * numNodule);
    LimitedIterable<ROI> limited =
        new LimitedIterable<>(noduleQuery.cloneQuery().iterator(), (int) numRequired);
    nodules = IterableUtils.chainedIterable(nodules, limited);

    return nodules;
  }

  public static void main(String[] args) throws Exception {
    new ArffGenerator().run();
  }

}
