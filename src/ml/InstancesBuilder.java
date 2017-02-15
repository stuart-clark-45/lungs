package ml;

import static model.roi.ROI.Class.NODULE;
import static model.roi.ROI.Class.NON_NODULE;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static weka.core.Utils.missingValue;

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

import ml.feature.CoarseHist;
import ml.feature.FineHist;
import ml.feature.MedHist;
import model.roi.ROI;
import util.MongoHelper;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Used to construct {@link Instances} from {@link ROI}s.
 *
 * @author Stuart Clark
 */
public class InstancesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstancesBuilder.class);
  private static final int LOG_INTERVAL = 5000;

  private ArrayList<Attribute> attributes;
  private final int numAttributes;
  private List<Function<ROI, Object>> functions;
  private Datastore ds;

  /**
   * True if the class should be set for the {@link Instance}, false otherwise
   */
  private boolean setClass;

  public InstancesBuilder(boolean setClass) {
    this.setClass = setClass;
    // Create list if attributes and methods to access them
    this.attributes = new ArrayList<>();
    this.functions = new ArrayList<>();

    // Add mean intensity
    this.attributes.add(new Attribute("Mean Intensity"));
    this.functions.add(ROI::getMeanIntensity);

    // Add area
    this.attributes.add(new Attribute("Area"));
    this.functions.add(ROI::getArea);

    // Add Perimeter
    this.attributes.add(new Attribute("Perimeter"));
    this.functions.add(ROI::getPerimLength);

    // Add Min Circle
    this.attributes.add(new Attribute("Min Circle Radius"));
    this.functions.add(roi -> roi.getMinCircle().getRadius());

    // Add Fitted Ellipse (Avoiding NPEs will ternary expression)
    this.attributes.add(new Attribute("Fitted Ellipse Angle"));
    this.functions.add(roi -> roi.getFitEllipse() != null ? roi.getFitEllipse().angle
        : missingValue());
    this.attributes.add(new Attribute("Fitted Ellipse Area"));
    this.functions.add(roi -> roi.getFitEllipse() != null ? roi.getFitEllipse().boundingRect()
        .area() : missingValue());
    this.attributes.add(new Attribute("Fitted Ellipse Width"));
    this.functions
        .add(roi -> roi.getFitEllipse() != null ? roi.getFitEllipse().boundingRect().width
            : missingValue());
    this.attributes.add(new Attribute("Fitted Ellipse Height"));
    this.functions
        .add(roi -> roi.getFitEllipse() != null ? roi.getFitEllipse().boundingRect().height
            : missingValue());

    // Add Coarse Histogram
    for (int i = 0; i < CoarseHist.BINS; i++) {
      this.attributes.add(new Attribute("Coarse Hist " + i));
      this.functions.add(roi -> roi.getCoarseHist().next());
    }

    // Add Medium Histogram
    for (int i = 0; i < MedHist.BINS; i++) {
      this.attributes.add(new Attribute("Medium Hist " + i));
      this.functions.add(roi -> roi.getMedHist().next());
    }

    // Add Fine Histogram
    for (int i = 0; i < FineHist.BINS; i++) {
      this.attributes.add(new Attribute("Fine Hist " + i));
      this.functions.add(roi -> roi.getFineHist().next());
    }

    // Add class
    this.attributes.add(new Attribute("Class", Arrays.asList(NODULE.name(), NON_NODULE.name())));
    this.functions.add(ROI::getClassification);

    this.numAttributes = attributes.size();

    this.ds = MongoHelper.getDataStore();
  }

  /**
   * @param name the name to give the {@link Instances}.
   * @param rois the list of {@link ROI}s to use.
   * @return {@link Instances} for the given {@link ROI}s
   */
  public Instances instances(String name, List<ROI> rois) {
    return instances(name, rois.iterator(), rois.size());
  }

  /**
   * @param name the name to give the {@link Instances}.
   * @param query the query to use to get the {@link ROI}s
   * @return {@link Instances} for the given {@link ROI}s
   */
  public Instances instances(String name, Query<ROI> query) {
    LOGGER.info("Creating Instances for " + name + "...");
    Instances instances = instances(name, query.iterator(), (int) query.count());

    // Log the number of each class in the instances
    LOGGER.info(name + " created with the following number of instances");
    Iterator<Result> results =
        ds.createAggregation(ROI.class).match(query)
            .group("classification", grouping("count", accumulator("$sum", 1)))
            .aggregate(Result.class);
    results.forEachRemaining(r -> LOGGER.info(r.toString()));

    return instances;
  }

  /**
   * @param name the name to give the {@link Instances}.
   * @param rois iterator for the {@link ROI}s.
   * @return {@link Instances} for the given {@link ROI}s
   */
  private Instances instances(String name, Iterator<ROI> rois, int numROI) {
    // Create instances
    Instances instances = new Instances(name, attributes, numROI);
    instances.setClassIndex(numAttributes - 1);

    // Iterate over rois
    int counter = 0;
    while (rois.hasNext()) {
      ROI roi = rois.next();

      // Add to the instances
      Instance instance = new DenseInstance(numAttributes);
      // Don't try to set class if setClass if false
      int end = setClass ? numAttributes : numAttributes - 1;
      for (int i = 0; i < end; i++) {
        setValue(instance, attributes.get(i), functions.get(i).apply(roi));
      }
      instances.add(instance);

      // Logging
      if (++counter % LOG_INTERVAL == 0) {
        LOGGER.info(counter + "/" + numROI + " " + name + " instances created");
      }
    }

    return instances;
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
    } else if (value instanceof Number) {
      instance.setValue(attribute, ((Number) value).doubleValue());
    } else if (value instanceof ROI.Class) {
      instance.setValue(attribute, ((ROI.Class) value).name());
    } else {
      throw new IllegalStateException(value.getClass()
          + " not yet supported by Trainer please add it");
    }
  }

  public ArrayList<Attribute> getAttributes() {
    return attributes;
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

}
