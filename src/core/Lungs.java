package core;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import ij.plugin.DICOM;
import model.CTStack;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;

public class Lungs {

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    CTStack ctStack = MongoHelper.getDataStore().createQuery(CTStack.class).get();

    List<Mat> mats = new ArrayList<>();
    for (String path : ctStack.getPaths()) {
      DICOM dicom = new DICOM();
      dicom.open(path);
      mats.add(MatUtils.fromDICOM(dicom));
    }

    import readings seperatley not just as fields of images

    new MatViewer(mats).display();
  }

}
