package ml;


import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ml.feature.AllHists;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.opencv.core.Point;

import ml.feature.Area;
import ml.feature.MeanIntensity;
import model.CTSlice;
import model.ROI;
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
    Testing.drop();

    ds = MongoHelper.getDataStore();

    String sopUid = "id";
    ROI roi = new ROI();
    roi.addPoint(new Point(0, 0));
    roi.addPoint(new Point(1, 1));
    roi.addPoint(new Point(1, 2));
    roi.setArea(3);
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
    new FeatureEngine(singletonList(new MeanIntensity()), singletonList(new AllHists())).run(es);
    ROI roi = ds.get(ROI.class, roiId);
    assertNotNull(roi.getMeanIntensity());
    assertNotNull(roi.getFineHist());
    assertNotNull(roi.getCoarseHist());
  }

}
