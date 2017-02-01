package data;

import static model.ReadingROI.Type.BIG_NODULE;
import static model.ReadingROI.Type.NON_NODULE;
import static model.ReadingROI.Type.SMALL_NODULE;

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
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import model.ReadingROI;
import model.lidc.EdgeMap;
import model.lidc.LidcReadMessage;
import model.lidc.Locus;
import model.lidc.NonNodule;
import model.lidc.ReadingSession;
import model.lidc.Roi;
import model.lidc.UnblindedReadNodule;
import util.LungsException;

/**
 * Used to CT scan readings created by radiologists.
 *
 * @author Stuart Clark
 */
public class ReadingROIImporter extends Importer<ReadingROI> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReadingROIImporter.class);
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

  public ReadingROIImporter() {
    super(ReadingROI.class);
  }

  @Override
  protected String testPath() {
    return "./testres/reading-roi-importer";
  }

  @Override
  protected String prodPath() {
    return "./resource/LIDC-XML-only";
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {
    try {
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

    } catch (Exception e) {
      throw new LungsException("Failed to import ReadingROIs", e);
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
    // Init the unmarshaller
    JAXBContext jaxb = JAXBContext.newInstance(ReadingSession.class);
    Unmarshaller unmarshaller = jaxb.createUnmarshaller();

    // Parse the document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xmlPath.toFile());

    // Create LidcReadMessage
    LidcReadMessage readMessage = new LidcReadMessage();
    List<ReadingSession> sessions = readMessage.getReadingSessions();

    // Populate sessions list
    NodeList children = doc.getFirstChild().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if (child.getNodeName().equals("readingSession")) {
        String nodeAsString = nodeToString(child);
        sessions.add((ReadingSession) unmarshaller.unmarshal(new StringReader(nodeAsString)));
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

  private void parseAndSaveReading(LidcReadMessage read, Datastore ds) throws LungsException {
    for (ReadingSession session : read.getReadingSessions()) {

      // Parse and save nodules
      for (UnblindedReadNodule nodule : session.getUnblindedReadNodule()) {
        parseAndSaveNodule(nodule, ds);
      }

      // Parse and save non-nodules
      for (NonNodule nonNodule : session.getNonNodule()) {
        parseAndSaveNonNodule(nonNodule, ds);
      }

    }
  }

  /**
   * Parse {@code nodule} to {@link ReadingROI}s and save them to the database.
   *
   * @param nodule
   * @param ds
   */
  private void parseAndSaveNodule(UnblindedReadNodule nodule, Datastore ds) throws LungsException {
    ObjectId groupId = new ObjectId();

    // Create a ReadingROI for each of the rois given by {@code nodule}
    for (Roi roi : nodule.getRoi()) {

      // Create the ReadingROI and set some fields
      ReadingROI readingROI = new ReadingROI();
      readingROI.setGroupId(groupId);
      readingROI.setImageSopUID(roi.getImageSOPUID());
      readingROI.setInclusive(Boolean.parseBoolean(roi.getInclusion()));

      // Covert edge map onto points
      List<Point> points = edgeMapsToEdgePoints(roi.getEdgeMap());

      // Set type and centroid (and edge points if big nodule)
      if (points.size() == 1) {
        readingROI.setType(SMALL_NODULE);
        readingROI.setCentroid(points.get(0));
        numSmallNodule++;
      } else {
        readingROI.setType(BIG_NODULE);
        readingROI.setEdgePoints(points);
        readingROI.setCentroid(calculateCentroid(points));
        numBigNodule++;
      }

      ds.save(readingROI);
    }
  }

  /**
   * Parse {@code nonNodule} to a {@link ReadingROI} and save it to the database.
   * 
   * @param nonNodule
   * @param ds
   */
  private void parseAndSaveNonNodule(NonNodule nonNodule, Datastore ds) {
    ObjectId groupId = new ObjectId();

    ReadingROI readingROI = new ReadingROI();
    readingROI.setType(NON_NODULE);
    readingROI.setGroupId(groupId);
    readingROI.setImageSopUID(nonNodule.getImageSOPUID());
    Locus locus = nonNodule.getLocus();
    readingROI.setCentroid(new Point(locus.getXCoord().doubleValue(), locus.getYCoord()
        .doubleValue()));
    numNonNodule++;

    ds.save(readingROI);
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

  public int getRejected() {
    return rejected;
  }

  public int getNumBigNodule() {
    return numBigNodule;
  }

  public int getNumSmallNodule() {
    return numSmallNodule;
  }

  public int getNumNonNodule() {
    return numNonNodule;
  }

  public static void main(String[] args) {
    new ReadingROIImporter().run();
  }

}
