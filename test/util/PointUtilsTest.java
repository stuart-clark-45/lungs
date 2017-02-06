package util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Point;

import static org.junit.Assert.assertEquals;

/**
 * @author Stuart Clark
 */
public class PointUtilsTest {

  private List<Point> unsorted = new ArrayList<>();
  private List<Point> sorted = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    unsorted = new ArrayList<>();
    unsorted.add(new Point(2, 2));
    unsorted.add(new Point(1, 2));
    unsorted.add(new Point(2, 1));
    unsorted.add(new Point(1, 1));

    sorted = new ArrayList<>();
    sorted.add(new Point(1, 1));
    sorted.add(new Point(2, 1));
    sorted.add(new Point(1, 2));
    sorted.add(new Point(2, 2));
  }

  @Test
  public void testXySort() throws Exception {
    assertEquals(sorted, PointUtils.xySort(unsorted));
  }

}
