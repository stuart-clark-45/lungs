package data;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Point;

import model.lidc.EdgeMap;
import model.lidc.LidcReadMessage;
import util.PreTest;

/**
 * @author Stuart Clark
 */
@RunWith(PreTest.class)
public class ReadingROIImporterTest {

  private ReadingROIImporter importer;

  @Before
  public void setUp() throws Exception {
    importer = new ReadingROIImporter();
  }

  @Test
  public void test() throws Exception {
    importer.run();
    assertEquals(2, importer.getRejected());
    assertEquals(240, importer.getNumBigNodule());
    assertEquals(38, importer.getNumSmallNodule());
    assertEquals(92, importer.getNumNonNodule());
  }

  @Test
  public void testUnmarshall() throws Exception {
    LidcReadMessage readMessage =
        new ReadingROIImporter().unmarshal(Paths.get("./testres/read-message.xml"));
    assertEquals(4, readMessage.getReadingSessions().size());
  }

  @Test
  public void testEdgeMapsToEdgePoints() throws Exception {
    List<EdgeMap> edgeMaps = new ArrayList<>();

    EdgeMap e1 = new EdgeMap();
    int e1x = 2;
    int e1y = 3;
    e1.setXCoord(BigInteger.valueOf(e1x));
    e1.setYCoord(BigInteger.valueOf(e1y));
    edgeMaps.add(e1);

    EdgeMap e2 = new EdgeMap();
    int e2x = 5;
    int e2y = 6;
    e2.setXCoord(BigInteger.valueOf(e2x));
    e2.setYCoord(BigInteger.valueOf(e2y));
    edgeMaps.add(e2);

    List<Point> points = importer.edgeMapsToEdgePoints(edgeMaps);
    assertEquals(2, points.size());

    Point p1 = points.get(0);
    assertEquals(e1x, (int) p1.x);
    assertEquals(e1y, (int) p1.y);

    Point p2 = points.get(1);
    assertEquals(e2x, (int) p2.x);
    assertEquals(e2y, (int) p2.y);
  }

  @Test
  public void testCalculateCentroid() throws Exception {
    List<Point> points = new ArrayList<>();
    points.add(new Point(1d, 4d));
    points.add(new Point(2d, 5d));
    points.add(new Point(3d, 6d));

    Point centroid = importer.calculateCentroid(points);
    assertEquals(Double.valueOf(2), Double.valueOf(centroid.x));
    assertEquals(Double.valueOf(5), Double.valueOf(centroid.y));
  }

}
