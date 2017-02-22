package util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Point;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class PointUtilsTest {

  @Test
  public void testPerim2Region() throws Exception {
    List<Point> perimeter = new ArrayList<>();
    perimeter.add(new Point(2, 6));
    perimeter.add(new Point(3, 5));
    perimeter.add(new Point(1, 5));
    perimeter.add(new Point(4, 4));
    perimeter.add(new Point(0, 4));
    perimeter.add(new Point(3, 3));
    perimeter.add(new Point(1, 3));
    perimeter.add(new Point(2, 2));

    // Test exclusive
    Set<Point> exclusive = new HashSet<>();
    exclusive.add(new Point(2, 3));
    exclusive.add(new Point(1, 4));
    exclusive.add(new Point(2, 4));
    exclusive.add(new Point(3, 4));
    exclusive.add(new Point(2, 5));
    assertEquals(exclusive, new HashSet<>(PointUtils.perim2Region(perimeter, false)));

    // Test inclusive
    Set<Point> inclusive = new HashSet<>(perimeter);
    inclusive.addAll(exclusive);
    assertEquals(inclusive, new HashSet<>(PointUtils.perim2Region(perimeter, true)));
  }

  @Test
  public void testRegion2Perim() throws Exception {
    // A shape with two contours
    List<Point> region = new ArrayList<>();
    region.add(new Point(4, 4));
    region.add(new Point(3, 5));
    region.add(new Point(4, 5));
    region.add(new Point(5, 5));
    region.add(new Point(2, 6));
    region.add(new Point(3, 6));
    region.add(new Point(4, 6));
    region.add(new Point(5, 6));
    region.add(new Point(6, 6));
    region.add(new Point(1, 7));
    region.add(new Point(2, 7));
    region.add(new Point(3, 7));
    region.add(new Point(5, 7));
    region.add(new Point(6, 7));
    region.add(new Point(7, 7));
    region.add(new Point(2, 8));
    region.add(new Point(3, 8));
    region.add(new Point(4, 8));
    region.add(new Point(5, 8));
    region.add(new Point(6, 8));
    region.add(new Point(3, 9));
    region.add(new Point(4, 9));
    region.add(new Point(5, 9));
    region.add(new Point(4, 10));

    // Expect only the outer most contour
    List<Point> expected = new ArrayList<>();
    expected.add(new Point(4, 4));
    expected.add(new Point(3, 5));
    expected.add(new Point(5, 5));
    expected.add(new Point(2, 6));
    expected.add(new Point(6, 6));
    expected.add(new Point(1, 7));
    expected.add(new Point(7, 7));
    expected.add(new Point(2, 8));
    expected.add(new Point(6, 8));
    expected.add(new Point(3, 9));
    expected.add(new Point(5, 9));
    expected.add(new Point(4, 10));

    HashSet<Point> actual = new HashSet<>(PointUtils.region2Contour(region));

    assertEquals(new HashSet<>(expected), actual);
  }

  @Test
  public void testXySort() throws Exception {
    List<Point> unsorted = new ArrayList<>();
    unsorted.add(new Point(2, 2));
    unsorted.add(new Point(1, 2));
    unsorted.add(new Point(2, 1));
    unsorted.add(new Point(1, 1));

    List<Point> sorted = new ArrayList<>();
    sorted.add(new Point(1, 1));
    sorted.add(new Point(2, 1));
    sorted.add(new Point(1, 2));
    sorted.add(new Point(2, 2));

    assertEquals(sorted, PointUtils.xySort(unsorted));
  }

  @Test
  public void testDeDupe() throws Exception {
    List<Point> points = new ArrayList<>();
    points.add(new Point(1, 1));
    points.add(new Point(1, 1));
    points.add(new Point(1, 1));
    points.add(new Point(2, 2));
    points.add(new Point(2, 2));
    points.add(new Point(2, 2));
    points.add(new Point(2, 2));
    points.add(new Point(3, 3));
    points.add(new Point(3, 3));
    points.add(new Point(3, 3));

    List<Point> expected = new ArrayList<>();
    expected.add(new Point(1, 1));
    expected.add(new Point(2, 2));
    expected.add(new Point(3, 3));

    assertEquals(expected, PointUtils.dedupe(points));
  }

}
