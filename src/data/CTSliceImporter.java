package data;

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
import model.CTSlice;
import util.ConfigHelper;
import util.LungsException;
import util.MongoHelper;

/**
 * Used to import information about CT cross sections into the database.
 *
 * @author Stuart Clark
 */
public class CTSliceImporter extends Importer<CTSlice> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CTSliceImporter.class);

  private static final String PROD_PATH = "./resource/DOI";
  private static final String TEST_PATH = "./testres/medical-image-importer";
  private static final int LOG_INTERVAL = 100;

  private String path;
  private Datastore ds;

  public CTSliceImporter() {
    super(CTSlice.class);

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

      LOGGER.info("Finished importing CTSlices");

    } catch (IOException e) {
      throw new LungsException("Failed to import models", e);
    }

  }

  private void parseAndSave(Path path) {
    String sPath = path.toString();

    DICOM dicom = new DICOM();
    String info = dicom.getInfo(sPath);

    String modality = stringForKey("0008,0060  Modality: ", info);
    if (!modality.equals("CT")) {
      return;
    }

    CTSlice slice = new CTSlice();
    slice.setFilePath(sPath);
    slice.setModel(modality);
    slice.setImageNumber(intForKey("0020,0013  Image Number: ", info));
    slice.setManufacturer(stringForKey("0008,0070  Manufacturer: ", info));
    slice.setModel(stringForKey("0008,1090  Manufacturer's Model Name: ", info));
    slice.setRows(intForKey("0028,0010  Rows: ", info));
    slice.setColumns(intForKey("0028,0011  Columns: ", info));
    slice.setkVp(intForKey("0018,0060  kVp: ", info));
    slice.setSliceLocation(doubleForKey("0020,1041  Slice Location: ", info));
    slice.setPatientId(stringForKey("0010,0020  Patient ID: ", info));
    slice.setSeriesInstanceUID(stringForKey("0020,000E  Series Instance UID: ", info));
    slice.setBitsAllocated(intForKey("0028,0100  Bits Allocated: ", info));
    slice.setBitsStored(intForKey("0028,0101  Bits Stored: ", info));
    slice.setHighBit(intForKey("0028,0102  High Bit: ", info));

    ds.save(slice);
  }

  /**
   * Used regex to get the integer value for the key.
   *
   * @param key all the text in the line of info before the actual value that is required.
   * @param info info obtained using {@link DICOM#getInfo(String)}.
   * @return the integer value for the key.
   */
  private Integer intForKey(String key, String info) {
    return Integer.parseInt(stringForKey(key, info));
  }

  /**
   * Used regex to get the double value for the key.
   *
   * @param key all the text in the line of info before the actual value that is required.
   * @param info info obtained using {@link DICOM#getInfo(String)}.
   * @return the double value for the key.
   */
  private Double doubleForKey(String key, String info) {
    return Double.parseDouble(stringForKey(key, info));
  }

  /**
   * Used regex to get the value for the key.
   *
   * @param key all the text in the line of info before the actual value that is required.
   * @param info info obtained using {@link DICOM#getInfo(String)}.
   * @return the value for the key.
   */
  private String stringForKey(String key, String info) {
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
    new CTSliceImporter().run();
  }

}
