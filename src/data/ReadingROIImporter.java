package data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import model.lidc.Roi;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import model.ReadingROI;
import model.lidc.LidcReadMessage;
import model.lidc.NonNodule;
import model.lidc.ReadingSession;
import model.lidc.UnblindedReadNodule;
import util.LungsException;

import static model.ReadingROI.Type.NODULE;

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

      // Init the unmarshaller
      JAXBContext jaxb = JAXBContext.newInstance(LidcReadMessage.class);
      Unmarshaller unmarshaller = jaxb.createUnmarshaller();

      // Parse each of the xmlFiles
      int filesUsed = 0;
      int counter = 0;
      for (Path path : xmlFiles) {
        if (isLidcReadMessage(path)) {
          LidcReadMessage read = (LidcReadMessage) unmarshaller.unmarshal(path.toFile());
          parseAndSaveReading(read, ds);
          filesUsed++;
        }

        if (++counter % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + numFiles + " xml files processed");
        }
      }

      LOGGER.info(numFiles + "/" + numFiles + " xml files processed");
      LOGGER.info(numFiles - filesUsed + "/" + numFiles + " xml files were rejected");

    } catch (JAXBException | IOException | SAXException | ParserConfigurationException e) {
      throw new LungsException("Failed to import ReadingROIs", e);
    }

  }

  /**
   * @param path the path to an xml document.
   * @return true if the root element of the document is an LidcReadMessage element, false
   *         otherwise.
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  private boolean isLidcReadMessage(Path path) throws IOException, SAXException,
      ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(path.toFile());
    return doc.getFirstChild().getNodeName().equals("LidcReadMessage");
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
//    readingROI.setImageSopUID();
  }

  private void parseAndSaveNonNodule(NonNodule nonNodule, Datastore ds) {

  }

  public static void main(String[] args) {
    new ReadingROIImporter().run();
  }

}
