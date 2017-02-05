package data;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Lungs;
import model.CTSlice;
import model.ROI;
import util.LungsException;
import util.MongoHelper;

/**
 * Used to import {@link ROI}s detected
 *
 * @author Stuart Clark
 */
public class ROIGenerator extends Importer<ROI> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIGenerator.class);

  /**
   * The {@link Query} used to obtain the {@link CTSlice}s that {@link ROI}s should be generated
   * for.
   */
  private Query<CTSlice> query;

  public ROIGenerator() {
    this(MongoHelper.getDataStore().createQuery(CTSlice.class).field("model").equal("Sensation 16"));
  }

  /**
   * @param query the {@link Query} used to obtain the {@link CTSlice}s that {@link ROI}s should be
   *        generated for.
   */
  public ROIGenerator(Query<CTSlice> query) {
    super(ROI.class);
    this.query = query;
  }

  @Override
  protected String testPath() {
    return null;
  }

  @Override
  protected String prodPath() {
    return null;
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {
    LOGGER.info("Generating ROIs this may take some time...");

    Lungs lungs = new Lungs();

    StreamSupport.stream(query.spliterator(), true).forEach(slice -> {
      Mat mat = Lungs.getSliceMat(slice);
      List<Mat> segmented = lungs.segment(Collections.singletonList(mat));
      try {
        lungs.roiExtraction(Collections.singletonList(slice), segmented);
      } catch (LungsException e) {
        LOGGER.error("Failed to extract ROI for stack with SOP UID: " + slice.getImageSopUID(), e);
      }
    });

    LOGGER.info("Finished generating ROIs");
  }
}
