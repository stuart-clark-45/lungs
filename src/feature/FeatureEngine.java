package feature;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Mat;

import core.Lungs;
import model.CTSlice;
import model.ROI;
import util.MongoHelper;

/**
 * Used to compute {@link Feature}s for {@link ROI}s.
 */
public class FeatureEngine implements Runnable {

  private List<Feature> features;
  private Query<ROI> query;
  private Datastore ds;

  public FeatureEngine() {
    this(defaultFeatures(), MongoHelper.getDataStore().createQuery(ROI.class));
  }

  public FeatureEngine(List<Feature> features, Query<ROI> query) {
    this.features = features;
    this.query = query;
    this.ds = MongoHelper.getDataStore();
  }

  @Override
  public void run() {

    StreamSupport.stream(query.spliterator(), true).forEach(
        roi -> {
          CTSlice slice =
              ds.createQuery(CTSlice.class).field("imageSopUID").equal(roi.getImageSopUID()).get();
          Mat mat = Lungs.getSliceMat(slice);

          for (Feature feature : features) {
            feature.compute(roi, mat);
          }

          ds.save(roi);
        });

  }

  /**
   * @return list of default features to use.
   */
  private static List<Feature> defaultFeatures() {
    List<Feature> features = new ArrayList<>();
    features.add(new MeanIntensity());
    return features;
  }

  public static void main(String[] args) {
    new FeatureEngine().run();
  }

}
