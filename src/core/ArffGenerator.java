package core;

import static model.ROI.Class.NODULE;
import static model.ROI.Class.NON_NODULE;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static util.DataFilter.TEST_INSTANCE;
import static util.DataFilter.TRAIN_INSTANCE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
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
  public static final String TRAIN_FILE = "train.arff";
  public static final String TEST_FILE = "test.arff";
  public static final String ALL_FILE = "all.arff";

  private Datastore ds;
  private ArrayList<Attribute> attributes;
  private List<Function<ROI, Object>> functions;

  public ArffGenerator() {
    ds = MongoHelper.getDataStore();
    // Create list if attributes and methods to access them
    attributes = new ArrayList<>();
    functions = new ArrayList<>();
    // Add mean intensity
    attributes.add(new Attribute("Mean Intensity"));
    functions.add(ROI::getMeanIntensity);
    // Add class
    attributes.add(new Attribute("Class", Arrays.asList(NODULE.name(), NON_NODULE.name())));
    functions.add(ROI::getClassification);
  }

  public void run() throws IOException {
    LOGGER.info("Running ArffGenerator...");

    // Create training set
    Query<ROI> trainQuery =
        ds.createQuery(ROI.class).field("seriesInstanceUID").equal(TRAIN_INSTANCE);
    Instances trainingSet = createInstances("Training Set", trainQuery);

    // Create testing set
    Query<ROI> testQuery =
        ds.createQuery(ROI.class).field("seriesInstanceUID").equal(TEST_INSTANCE);
    Instances testingSet = createInstances("Testing Set", testQuery);

    // Combine testing and training sets
    Instances all = new Instances("Combined Set", attributes, trainingSet.size() + testingSet.size());
    all.addAll(trainingSet);
    all.addAll(testingSet);

    // Create arff files
    save(trainingSet, TRAIN_FILE);
    save(testingSet, TEST_FILE);
    save(all, ALL_FILE);

    LOGGER.info("ArffGenerator finished running");
  }

  /**
   * @param name the name to give the set of {@link Instances}.
   * @param query
   * @return {@link Instances} for the given {@link ROI}s
   */
  private Instances createInstances(String name, Query<ROI> query) {
    int numROI = (int) query.count();
    int numAttributes = attributes.size();

    // Create set
    Instances set = new Instances(name, attributes, numROI);
    set.setClassIndex(numAttributes - 1);

    LOGGER.info("Creating Instances for " + name + "...");
    int counter = 0;
    for (ROI roi : query) {

      // Create the Instance
      Instance instance = new DenseInstance(numAttributes);
      for (int i = 0; i < numAttributes; i++) {
        setValue(instance, attributes.get(i), functions.get(i).apply(roi));
      }

      // Add to the set
      set.add(instance);

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numROI + " " + name + " instances created");
      }
    }

    // Log the number of each class in the instances
    LOGGER.info(name + " created with the following number of instances");
    Iterator<Result> results =
        ds.createAggregation(ROI.class).match(query)
            .group("classification", grouping("count", accumulator("$sum", 1)))
            .aggregate(Result.class);
    results.forEachRemaining(r -> LOGGER.info(r.toString()));

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

  /**
   * Set {@code attribute} {@code value} for the {@code instance} by casting it to the correct type.
   * 
   * @param instance
   * @param attribute
   * @param value
   */
  private static void setValue(Instance instance, Attribute attribute, Object value) {
    if (value == null) {
      throw new IllegalStateException("Value for " + attribute.name()
          + " is null you may need to run the FeatureEngine again");
    } else if (value instanceof Double) {
      instance.setValue(attribute, (Double) value);
    } else if (value instanceof ROI.Class) {
      instance.setValue(attribute, ((ROI.Class) value).name());
    } else {
      throw new IllegalStateException(value.getClass()
          + " not yet supported by Trainer please add it");
    }
  }

  /**
   * Used to obtain the results of an aggregation counting the occurrences of different classes of
   * ROIs.
   */
  private static class Result {

    @Id
    private String id;

    private int count;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    @Override
    public String toString() {
      return id + ": " + count;
    }

  }

  public static void main(String[] args) throws IOException {
    new ArffGenerator().run();
  }

}
