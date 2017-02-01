package data;

import static model.ReadingROI.Type.NODULE;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import model.ReadingROI;
import model.lidc.LidcReadMessage;
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

      // Parse each of the xmlFiles
      int filesUsed = 0;
      int counter = 0;
      for (Path xmlPath : xmlFiles) {
        if (isLidcReadMessage(xmlPath)) {
          parseAndSaveReading(unmarshal(xmlPath), ds);
          filesUsed++;
        } else {
          LOGGER.info("REJECTED " + xmlPath);
        }

        if (++counter % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + numFiles + " xml files processed");
        }
      }

      LOGGER.info(numFiles + "/" + numFiles + " xml files processed");
      LOGGER.info(numFiles - filesUsed + "/" + numFiles + " xml files were rejected");

    } catch (Exception e) {
      throw new LungsException("Failed to import ReadingROIs", e);
    }

  }

  /**
   * @param xmlPath the path to an xml document.
   * @return true if the root element of the document is an LidcReadMessage element, false
   *         otherwise.
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  private boolean isLidcReadMessage(Path xmlPath) throws IOException, SAXException,
      ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xmlPath.toFile());
    return doc.getFirstChild().getNodeName().equals("LidcReadMessage");
  }

  public LidcReadMessage unmarshal(Path xmlPath) throws Exception {
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

    return readMessage;
  }

  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }

  private void parseAndSaveReading(LidcReadMessage read, Datastore ds) {
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

  private void parseAndSaveNodule(UnblindedReadNodule noduleRead, Datastore ds) {
    List<Roi> rois = noduleRead.getRoi();

    ReadingROI readingROI = new ReadingROI(NODULE);
    // readingROI.setImageSopUID();
  }

  private void parseAndSaveNonNodule(NonNodule nonNodule, Datastore ds) {

  }

  public static void main(String[] args) {
    new ReadingROIImporter().run();
  }

}
