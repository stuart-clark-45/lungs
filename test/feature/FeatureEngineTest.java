package feature;

import static junit.framework.TestCase.assertNotNull;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import model.CTSlice;
import model.ROI;
import org.opencv.core.Point;
import util.MongoHelper;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class FeatureEngineTest {

  private Datastore ds;
  private ObjectId roiId;

  @Before
  public void setUp() throws Exception {
    ds = MongoHelper.getDataStore();

    String sopUid = "id";
    ROI roi = new ROI();
    roi.addPoint(new Point(0,0));
    roi.addPoint(new Point(1,1));
    roi.addPoint(new Point(1,2));
    roi.setImageSopUID(sopUid);
    ds.save(roi);
    roiId = roi.getId();

    CTSlice slice = new CTSlice();
    slice.setImageSopUID(sopUid);
    slice.setFilePath("./testres/test.dcm");
    ds.save(slice);
  }

  @After
  public void tearDown() throws Exception {
    Testing.drop();
  }

  @Test
  public void test() throws Exception {
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Query<ROI> query = ds.createQuery(ROI.class).field("_id").equal(roiId);

    new FeatureEngine(es, Collections.singletonList(new MeanIntensity()), query).run();

    ROI roi = query.get();

    assertNotNull(roi.getMeanIntensity());
  }

}
