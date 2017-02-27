package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author Stuart Clark
 */
public class BatchIteratorTest {

  @Test
  public void test() throws Exception {
    List<String> input = Arrays.asList("1", "2", "3");
    BatchIterator<String> iterator = new BatchIterator<>(input.iterator(), 2);

    List<String> output = new ArrayList<>();
    while (iterator.hasNext()) {
      output.add(iterator.next());
    }
    assertEquals(Arrays.asList("1", "2"), output);
    assertNull(iterator.next());
    assertFalse(iterator.isFinished());

    iterator.nextBatch();
    output = new ArrayList<>();
    while (iterator.hasNext()) {
      output.add(iterator.next());
    }
    assertEquals(Collections.singletonList("3"), output);
    assertNull(iterator.next());
    assertTrue(iterator.isFinished());
  }

}
