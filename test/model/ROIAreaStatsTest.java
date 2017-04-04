package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;

import util.MongoHelper;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class ROIAreaStatsTest {

  private Datastore ds;

  @Before
  public void setUp() throws Exception {
    Testing.drop();

    ds = MongoHelper.getDataStore();

    for (int i = 1; i <= 10; i++) {
      ROI roi = new ROI();
      roi.setArea(i);
      ds.save(roi);
    }

  }

  @After
  public void tearDown() throws Exception {
    Testing.drop();
  }

  @Test
  public void test() throws Exception {
    ROIAreaStats.compute();
    ROIAreaStats stats = ROIAreaStats.get();
    assertEquals(1, stats.getMin());
    assertEquals(Double.valueOf(5.5), Double.valueOf(stats.getMean()));
    assertEquals(10, stats.getMax());
    assertEquals(1, ds.createQuery(ROIAreaStats.class).count());

    // Do it again to make sure there is still only one in the DB
    ROIAreaStats.compute();
    assertEquals(1, ds.createQuery(ROIAreaStats.class).count());

    // Test get()
    assertNotNull(ROIAreaStats.get());
  }

  @Test(expected = IllegalStateException.class)
  public void testNotPresent() throws Exception {
    Testing.drop();
    ROIAreaStats.clear();
    ROIAreaStats.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testNotROIs() throws Exception {
    Testing.drop();
    ROIAreaStats.compute();
  }

}
