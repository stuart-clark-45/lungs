package importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Mode;
import ij.plugin.DICOM;
import model.MedicalImage;
import util.ConfigHelper;
import util.LungsException;
import util.MongoHelper;

/**
 * Used to import information about medical images into the database
 *
 * @author Stuart Clark
 */
public class MedicalImageImporter extends Importer<MedicalImage> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MedicalImageImporter.class);

  private static final String PROD_PATH = "./resource/DOI";
  private static final String TEST_PATH = "./testres/medical-image-importer";
  private static final int LOG_INTERVAL = 100;

  private String path;
  private Datastore ds;

  public MedicalImageImporter() {
    super(MedicalImage.class);

    Mode.VALUE mode = ConfigHelper.getMode();
    if (mode == Mode.VALUE.PROD) {
      path = PROD_PATH;
    } else {
      path = TEST_PATH;
    }

    ds = MongoHelper.getDataStore();
  }

  @Override
  protected void importModels(Datastore ds) throws LungsException {

    try {
      List<Path> dicomFiles =
          Files.find(Paths.get(path), Integer.MAX_VALUE,
              (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".dcm"))
              .collect(Collectors.toList());

      int counter = 0;
      for (Path dicomFile : dicomFiles) {
        parseAndSave(dicomFile);
        if (counter++ % LOG_INTERVAL == 0) {
          LOGGER.info(counter + "/" + dicomFiles.size() + " imported");
        }
      }

      LOGGER.info("Finished importing MedicalImages");

    } catch (IOException e) {
      throw new LungsException("Failed to import models", e);
    }

  }

  private void parseAndSave(Path path) {
    String sPath = path.toString();

    DICOM dicom = new DICOM();
    String info = dicom.getInfo(sPath);

    MedicalImage image = new MedicalImage();
    image.setImageNumber(imageNumber(info));

    ds.save(image);
  }

  private int imageNumber(String info) {
    String key = "0020,0013  Image Number: ";
    return Integer.parseInt(valueForKey(key, info));
  }

  /**
   * Used regex to get the value for the key.
   *
   * @param key all the text in the line of info before the actual value that is required.
   * @param info info obtained using {@link DICOM#getInfo(String)}.
   * @return the value for the key.
   */
  private String valueForKey(String key, String info) {
    Matcher m = Pattern.compile(key + ".*").matcher(info);

    String line = null;

    int counter = 0;
    while (m.find()) {
      line = m.group();
      counter++;
    }

    if (counter != 1) {
      throw new IllegalStateException(counter + " lines with \"" + key + "\" not 1");
    }
    return line.substring(key.length()).trim();
  }

  public static void main(String[] args) throws Exception {
    new MedicalImageImporter().call();
  }

}
