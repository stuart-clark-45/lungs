package util;

import static org.junit.Assert.assertEquals;
import static util.DataFilter.TEST_INSTANCE;
import static util.DataFilter.TRAIN_INSTANCE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import config.Mode;
import model.CTStack;

/**
 * @author Stuat Clark
 */
@RunWith(Testing.class)
public class DataFilterTest {

  private Datastore ds;
  private List<String> uids;

  @Before
  public void setUp() throws Exception {
    ds = MongoHelper.getDataStore();

    // Create and save some stacks
    uids = Arrays.asList(TEST_INSTANCE, TRAIN_INSTANCE, "3", "4");
    uids.forEach(uid -> {
      CTStack stack = new CTStack();
      stack.setSeriesInstanceUID(uid);
      stack.setModel(DataFilter.MODEL);
      ds.save(stack);
    });

    // Create and save a CTStack that should be filtered out
    CTStack ignore = new CTStack();
    ignore.setSeriesInstanceUID("some id");
    ignore.setModel("some model");
    ds.save(ignore);
  }

  @After
  public void tearDown() throws Exception {
    Testing.drop();
  }

  @Test
  public void testProdMode() throws Exception {
    // Go into prod mode
    ConfigHelper.getProps().put(Mode.KEY, Mode.Value.PROD.name());

    // Calculate the expected size of the train and test sets
    int total = uids.size();
    int trainSize = Double.valueOf(total * DataFilter.TRAIN_WEIGHT).intValue();
    int testSize = total - trainSize;

    DataFilter filter = new DataFilter();

    // Check the aggregation correctly split the instances
    assertEquals(trainSize, filter.trainInstances.size());
    assertEquals(testSize, filter.testInstances.size());
    assertEquals(total, filter.allInstances.size());

    // Test all(..)
    Set<String> expectedAll = new HashSet<>(uids);
    Set<String> all = getUIDs(filter.all(ds.createQuery(CTStack.class)));
    assertEquals(expectedAll, all);

    // Test train(..)
    Set<String> expectedTrain = new HashSet<>(filter.trainInstances);
    Set<String> train = getUIDs(filter.train(ds.createQuery(CTStack.class)));
    assertEquals(expectedTrain, train);

    // Test test(..)
    Set<String> expectedTest = new HashSet<>(filter.testInstances);
    Set<String> test = getUIDs(filter.test(ds.createQuery(CTStack.class)));
    assertEquals(expectedTest, test);

    // Go back to test mode
    ConfigHelper.getProps().put(Mode.KEY, Mode.Value.TEST.name());
  }

  @Test
  public void testOtherModes() throws Exception {
    DataFilter filter = DataFilter.get();

    Set<String> expectedAll = Stream.of(TRAIN_INSTANCE, TEST_INSTANCE).collect(Collectors.toSet());
    Set<String> all = getUIDs(filter.all(ds.createQuery(CTStack.class)));
    assertEquals(expectedAll, all);

    Query<CTStack> train = filter.train(ds.createQuery(CTStack.class));
    assertEquals(1, train.count());
    assertEquals(TRAIN_INSTANCE, train.get().getSeriesInstanceUID());

    Query<CTStack> test = filter.test(ds.createQuery(CTStack.class));
    assertEquals(1, test.count());
    assertEquals(TEST_INSTANCE, test.get().getSeriesInstanceUID());
  }

  private Set<String> getUIDs(Query<CTStack> query) {
    return query.asList().stream().map(CTStack::getSeriesInstanceUID).collect(Collectors.toSet());
  }

}
