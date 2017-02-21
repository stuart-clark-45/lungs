package data;

import static model.GroundTruth.Type.BIG_NODULE;
import static model.GroundTruth.Type.NON_NODULE;
import static model.GroundTruth.Type.SMALL_NODULE;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import config.Misc;
import model.GroundTruth;
import model.lidc.EdgeMap;
import model.lidc.LidcReadMessage;
import model.lidc.Locus;
import model.lidc.NonNodule;
import model.lidc.ReadingSession;
import model.lidc.ResponseHeader;
import model.lidc.Roi;
import model.lidc.UnblindedReadNodule;
import util.ConfigHelper;
import util.LungsException;
import util.PointUtils;

/**
 * Used to import CT scan readings created by radiologists.
 *
 * @author Stuart Clark
 */
public class GroundTruthImporter extends Importer<GroundTruth> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroundTruthImporter.class);
  private static final int LOG_INTERVAL = 100;

  /**
   * The number of files that were rejected by the importer.
   */
  private int rejected;

  /**
   * The number of big nodules imported.
   */
  private int numBigNodule;

  /**
   * The number of small nodule imported.
   */
  private int numSmallNodule;

  /**
   * The number of non-nodules imported.
   */
  private int numNonNodule;

  public GroundTruthImporter() {
    super(GroundTruth.class);
  }

  @Override
  protected String testPath() {
    return "./testres/ground-truth-importer";
  }

  @Override
  protected String normalPath() {
    return ConfigHelper.getString(Misc.LIDC) + "/LIDC-XML-only";
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {
    try {
      LOGGER.info("Importing GroundTruths...");

      // Recursively find all the xml files
      List<Path> xmlFiles =
          Files.find(Paths.get(path), Integer.MAX_VALUE,
              (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".xml"))
              .collect(Collectors.toList());
      int numFiles = xmlFiles.size();

      // Parse each of the xmlFiles, rejecting files that are not for CT scan images
      int counter = 0;
      for (Path xmlPath : xmlFiles) {
        boolean isCTScan = new String(Files.readAllBytes(xmlPath)).contains("LidcReadMessage");
        if (isCTScan) {
          parseAndSaveReading(unmarshal(xmlPath), ds);
        } else {
          LOGGER.debug("REJECTED " + xmlPath);
          rejected++;
        }

        if (++counter % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + numFiles + " xml files processed");
        }
      }

      LOGGER.info(numFiles + "/" + numFiles + " xml files processed");
      LOGGER.info(rejected + "/" + numFiles + " xml files were rejected");
      LOGGER.info("Finished importing GroundTruths");

    } catch (Exception e) {
      throw new LungsException("Failed to import GroundTruths", e);
    }

  }

  /**
   * Unmarshal the xml file at {@code xmlPath}.
   * 
   * @param xmlPath
   * @return the unmarshalled file.
   * @throws Exception
   */
  LidcReadMessage unmarshal(Path xmlPath) throws Exception {
    // Init the unmarshallers
    Unmarshaller sessionParser = JAXBContext.newInstance(ReadingSession.class).createUnmarshaller();
    Unmarshaller headerParser = JAXBContext.newInstance(ResponseHeader.class).createUnmarshaller();

    // Parse the document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xmlPath.toFile());

    // Create LidcReadMessage
    LidcReadMessage readMessage = new LidcReadMessage();
    List<ReadingSession> sessions = readMessage.getReadingSessions();

    // Build readMessage
    NodeList children = doc.getFirstChild().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if (child.getNodeName().equals("readingSession")) {
        String nodeAsString = nodeToString(child);
        sessions.add((ReadingSession) sessionParser.unmarshal(new StringReader(nodeAsString)));
      }

      if (child.getNodeName().equals("ResponseHeader")) {
        String nodeAsString = nodeToString(child);
        readMessage.setResponseHeader((ResponseHeader) headerParser.unmarshal(new StringReader(
            nodeAsString)));
      }

    }

    // Check the file has been processed correctly
    if (sessions.isEmpty()) {
      throw new LungsException("NO ReadingSessions found in: " + xmlPath);
    }

    return readMessage;
  }

  /**
   * @param node
   * @return the xml for the given {@code node}.
   * @throws TransformerException
   */
  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }

  /**
   * Parse {@code read} to a {@link GroundTruth} and save it to the database.
   * 
   * @param read
   * @param ds
   * @throws LungsException
   */
  private void parseAndSaveReading(LidcReadMessage read, Datastore ds) throws LungsException {
    List<ReadingSession> readingSessions = read.getReadingSessions();
    for (int i = 0; i < readingSessions.size(); i++) {
      ReadingSession session = readingSessions.get(i);
      String seriesInstanceUid = read.getResponseHeader().getSeriesInstanceUID();

      // Parse and save nodules
      for (UnblindedReadNodule nodule : session.getUnblindedReadNodule()) {

        parseAndSaveNodule(nodule, ds, i, seriesInstanceUid);
      }

      // Parse and save non-nodules
      for (NonNodule nonNodule : session.getNonNodule()) {
        parseAndSaveNonNodule(nonNodule, ds, i, seriesInstanceUid);
      }

    }
  }

  /**
   * Parse {@code nodule} to {@link GroundTruth}s and save them to the database.
   * 
   * @param nodule
   * @param ds
   * @param readingNumber
   * @param seriesInstanceUid
   */
  private void parseAndSaveNodule(UnblindedReadNodule nodule, Datastore ds, int readingNumber,
      String seriesInstanceUid) throws LungsException {
    ObjectId groupId = new ObjectId();

    // Create a GroundTruth for each of the rois given by {@code nodule}
    for (Roi roi : nodule.getRoi()) {

      // Create the GroundTruth and set some fields
      GroundTruth groundTruth = new GroundTruth();
      groundTruth.setGroupId(groupId);
      groundTruth.setImageSopUID(roi.getImageSOPUID());
      groundTruth.setReadingNumber(readingNumber);
      groundTruth.setSeriesInstanceUID(seriesInstanceUid);
      boolean inclusive = Boolean.parseBoolean(roi.getInclusion());
      groundTruth.setInclusive(inclusive);

      // Covert edge map onto points
      List<Point> points = edgeMapsToEdgePoints(roi.getEdgeMap());

      // Set type and centroid (and edge points if big nodule)
      if (points.size() == 1) {
        groundTruth.setType(SMALL_NODULE);
        groundTruth.setCentroid(points.get(0));
        numSmallNodule++;
      } else {
        groundTruth.setType(BIG_NODULE);
        groundTruth.setCentroid(calculateCentroid(points));

        // Set the region and the edge points and min radius.
        List<Point> region = PointUtils.perim2Region(points, inclusive);
        groundTruth.setRegion(region);
        groundTruth.setEdgePoints(points);
        // -1 for not inclusive as edge pixel pixel should not be included in radius
        float minRadius = inclusive ? computeMinRadius(points) : computeMinRadius(points) - 1;
        groundTruth.setMinRadius(minRadius);

        numBigNodule++;

      }

      ds.save(groundTruth);
    }
  }

  /**
   * Parse {@code nonNodule} to a {@link GroundTruth} and save it to the database.
   * 
   * @param nonNodule
   * @param ds
   * @param readingNumber
   * @param seriesInstanceUid
   */
  private void parseAndSaveNonNodule(NonNodule nonNodule, Datastore ds, int readingNumber,
      String seriesInstanceUid) {
    ObjectId groupId = new ObjectId();

    GroundTruth groundTruth = new GroundTruth();
    groundTruth.setType(NON_NODULE);
    groundTruth.setGroupId(groupId);
    groundTruth.setImageSopUID(nonNodule.getImageSOPUID());
    groundTruth.setReadingNumber(readingNumber);
    groundTruth.setSeriesInstanceUID(seriesInstanceUid);
    Locus locus = nonNodule.getLocus();
    groundTruth.setCentroid(new Point(locus.getXCoord().doubleValue(), locus.getYCoord()
        .doubleValue()));
    numNonNodule++;

    ds.save(groundTruth);
  }

  /**
   * @param edgeMaps
   * @return a list of points created using {@code edgeMaps}.
   * @throws LungsException if an x or y co-ordinate is null.
   */
  List<Point> edgeMapsToEdgePoints(List<EdgeMap> edgeMaps) throws LungsException {
    List<Point> edgePoints = new ArrayList<>();
    for (EdgeMap map : edgeMaps) {
      if (map.getXCoord() == null || map.getYCoord() == null) {
        throw new LungsException("Edgemap coordinates cannot be null");
      }
      edgePoints.add(new Point(map.getXCoord().doubleValue(), map.getYCoord().doubleValue()));
    }
    return edgePoints;
  }

  /**
   * @param points
   * @return the centroid of the list of points given.
   */
  Point calculateCentroid(List<Point> points) {
    double x = 0;
    double y = 0;
    for (Point point : points) {
      x += point.x;
      y += point.y;
    }
    int nPoints = points.size();
    x = x / nPoints;
    y = y / nPoints;
    return new Point(x, y);
  }

  /**
   * @param contour the contour of the ground truth
   * @return the radius of the smallest circle that can be fitted to {@code contour}.
   */
  float computeMinRadius(List<Point> contour) {
    Point center = new Point();
    MatOfPoint2f matOfPoints = new MatOfPoint2f();
    matOfPoints.fromList(contour);
    float[] radius = new float[1];
    Imgproc.minEnclosingCircle(matOfPoints, center, radius);
    return radius[0];
  }

  int getRejected() {
    return rejected;
  }

  int getNumBigNodule() {
    return numBigNodule;
  }

  int getNumSmallNodule() {
    return numSmallNodule;
  }

  int getNumNonNodule() {
    return numNonNodule;
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    new GroundTruthImporter().run();
  }

}
