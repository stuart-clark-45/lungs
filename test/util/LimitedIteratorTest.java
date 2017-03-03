package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Stuart Clark
 */
public class LimitedIteratorTest {

  @Test
  public void test() throws Exception {
    List<String> input = Arrays.asList("1", "2", "3");
    LimitedIterator<String> iterator = new LimitedIterator<>(input.iterator(), 2);

    List<String> output = new ArrayList<>();
    while (iterator.hasNext()) {
      output.add(iterator.next());
    }
    assertEquals(Arrays.asList("1", "2"), output);

    assertNull(iterator.next());
  }

}
