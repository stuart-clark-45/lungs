package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;

import model.CTSlice;
import model.CTStack;
import util.MongoHelper;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class CTStackGeneratorTest {

  private Datastore ds;

  @Before
  public void setUp() throws Exception {
    ds = MongoHelper.getDataStore();
    ds.getCollection(CTSlice.class).drop();
  }

  @After
  public void tearDown() throws Exception {
    Testing.drop();
  }

  @Test
  public void test() throws Exception {
    // This stack should be dropped when the generator is run
    CTStack ctStack = new CTStack();
    ds.save(ctStack);

    // Create a series of slices
    String series1 = "s1";
    CTSlice slice = new CTSlice();
    slice.setImageNumber(3);
    slice.setSeriesInstanceUID(series1);
    ds.save(slice);
    slice = new CTSlice();
    slice.setImageNumber(2);
    slice.setSeriesInstanceUID(series1);
    ds.save(slice);
    slice = new CTSlice();
    slice.setImageNumber(1);
    slice.setSeriesInstanceUID(series1);
    ds.save(slice);

    // Create another series of slices
    String series2 = "s2";
    slice = new CTSlice();
    slice.setImageNumber(1);
    slice.setSeriesInstanceUID(series2);
    ds.save(slice);
    slice = new CTSlice();
    slice.setImageNumber(2);
    slice.setSeriesInstanceUID(series2);
    ds.save(slice);

    // Run the generator
    new CTStackGenerator().run();

    // Check the collection was dropped when generator was run
    assertNull(ds.get(CTStack.class, ctStack.getId()));

    // Get series1
    List<CTStack> stacks =
        ds.createQuery(CTStack.class).field("seriesInstanceUID").equal(series1).asList();

    // Check only one stack per series id
    assertEquals(1, stacks.size());

    // Check correct number of slices in stack
    List<CTSlice> stack1 = stacks.get(0).getSlices();
    assertEquals(3, stack1.size());

    // Check slices are in correct order
    assertEquals(Integer.valueOf(1), stack1.get(0).getImageNumber());
    assertEquals(Integer.valueOf(2), stack1.get(1).getImageNumber());
    assertEquals(Integer.valueOf(3), stack1.get(2).getImageNumber());

    // Get series2
    stacks = ds.createQuery(CTStack.class).field("seriesInstanceUID").equal(series2).asList();

    // Check only one stack per series id
    assertEquals(1, stacks.size());

    // Check correct number of slices in stack
    List<CTSlice> stack2 = stacks.get(0).getSlices();
    assertEquals(2, stack2.size());

    // Check slices are in correct order
    assertEquals(Integer.valueOf(1), stack1.get(0).getImageNumber());
    assertEquals(Integer.valueOf(2), stack1.get(1).getImageNumber());
  }

}
