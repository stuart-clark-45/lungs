package ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ROI;
import util.LimitedIterator;
import util.MongoHelper;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils;
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
    Instances trainData = ConverterUtils.DataSource.read(TRAIN_FILE);
    trainData.setClassIndex(trainData.numAttributes() - 1);

    // Build classifier
    Classifier cls = new J48();
    cls.buildClassifier(trainData);

    // Load testData
    BufferedReader reader = new BufferedReader(new FileReader(TEST_FILE));
    ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader, 0);
    Instances testData = arff.getStructure();
    testData.setClassIndex(testData.numAttributes() - 1);

    // Incrementally classify each of the instances in testData
    Evaluation eval = new Evaluation(testData);
    Instance inst;
    while ((inst = arff.readInstance(testData)) != null) {
      eval.evaluationForSingleInstance(cls.distributionForInstance(inst), inst, true);
    }

    // Evaluate classifier and print some statistics
    eval.evaluateModel(cls, testData);
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
   * @param limitOn true if the number of nodules and non-nodules should be the same in the
   *        {@link Instances} returned, false is all the non-nodules available should be used.
   * @return
   */
  private void createFile(String name, String file, ROI.Set set, boolean limitOn)
      throws IOException {
    LOGGER.info("Creating " + name + "...");

    // Find all the nodules
    LOGGER.info("Finding all NODULE for " + name);
    Query<ROI> noduleQuery =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NODULE);
    int numNodules = (int) noduleQuery.count();

    // Find all the non nodules
    LOGGER.info("Finding all NON_NODULE for " + name);
    Query<ROI> nonNoduleQuery =
        ds.createQuery(ROI.class).field(SET).equal(set).field(CLASS).equal(ROI.Class.NON_NODULE);
    int numNonNodule;
    if (limitOn) {
      numNonNodule = numNodules;
    } else {
      numNonNodule = (int) nonNoduleQuery.count();
    }

    // Log the size of the classes
    LOGGER.info(name + " will have:\n" + numNodules + " NODULES\n" + numNonNodule + " NON_NODULES");

    ArffSaver saver = new ArffSaver();
    saver.setFile(new File(file));
    saver.setRetrieval(Saver.INCREMENTAL);
    Instances instances = builder.createSet(name, numNodules + numNonNodule);
    saver.setStructure(instances);

    // Write all the nodules to the arff file
    int counter = 0;
    for (ROI roi : noduleQuery) {
      Instance instance = builder.createInstance(roi);
      instance.setDataset(instances);
      saver.writeIncremental(instance);

      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numNonNodule + " NODULES have been added to the arff file");
      }
    }

    // Write all the non-nodules to the arff file
    counter = 0;
    LimitedIterator<ROI> nonNodules =
        new LimitedIterator<>(nonNoduleQuery.iterator(), numNonNodule);
    while (nonNodules.hasNext()) {
      Instance instance = builder.createInstance(nonNodules.next());
      instance.setDataset(instances);
      saver.writeIncremental(instance);

      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numNonNodule + " NON_NODULES have been added to the arff file");
      }
    }

    // Flushes the instances to the file
    saver.writeIncremental(null);
  }

  public static void main(String[] args) throws Exception {
    new ArffGenerator().run();
  }

}
