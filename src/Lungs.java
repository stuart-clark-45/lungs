import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ij.plugin.DICOM;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import util.MatUtils;
import util.MatViewer;

public class Lungs {


  private static final String DIR =
      "resource/DOI/LIDC-IDRI-0029/1.3.6.1.4.1.14519.5.2.1.6279.6001.788972240715000723677133060452/1.3.6.1.4.1.14519.5.2.1.6279.6001.264090899378396711987322794314/";

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    File[] dcmFiles = new File(DIR).listFiles((dir, name) -> name.endsWith(".dcm"));

    List<Mat> mats = new ArrayList<>();
    List<String> names = new ArrayList<>();

    for (File file : dcmFiles) {
      DICOM dicom = new DICOM();
      dicom.open(file.getPath());
      mats.add(MatUtils.fromDICOM(dicom));
      names.add(file.getName());
    }

    new MatViewer(mats, names).display();
  }

}
