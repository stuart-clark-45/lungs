package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Stuart Clark
 */
public class CounterTest {

  @Test
  public void test() throws Exception {

    // Instantiate the counter
    Counter<Integer> counter = new Counter<>();

    // Add some 1s
    final int numOnes = 3;
    for (int i = 0; i < numOnes; i++) {
      counter.add(1);
    }

    // Add some 2s
    final int numTwos = 2;
    for (int i = 0; i < numTwos; i++) {
      counter.add(2);
    }

    // Check that counter has the keys
    assertTrue(counter.containsKey(1));
    assertTrue(counter.containsKey(2));

    // Check the counts were correct
    assertEquals(Integer.valueOf(numOnes), counter.get(1));
    assertEquals(Integer.valueOf(numTwos), counter.get(2));
  }

}
