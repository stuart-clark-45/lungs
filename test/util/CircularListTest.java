package util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * @author Stuart Clark
 */
public class CircularListTest {

  private static final Integer ZERO = 0;
  private static final int HUNDRED = 1;

  @Test
  public void test() throws Exception {
    List<Integer> ints = new CircularList<>();
    ints.add(ZERO);
    assertEquals(ZERO, ints.get(ZERO));
    assertEquals(ZERO, ints.get(HUNDRED));
    assertEquals(ZERO, ints.get(-HUNDRED));

    ints = new CircularList<>(HUNDRED);
    ints.add(ZERO);
    assertEquals(ZERO, ints.get(ZERO));
    assertEquals(ZERO, ints.get(HUNDRED));
    assertEquals(ZERO, ints.get(-HUNDRED));

    ints = new CircularList<>(ints);
    assertEquals(ZERO, ints.get(ZERO));
    assertEquals(ZERO, ints.get(HUNDRED));
    assertEquals(ZERO, ints.get(-HUNDRED));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testEmpty() throws Exception {
    CircularList<Object> list = new CircularList<>();
    list.get(ZERO);
  }

}
