package core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import model.CTStack;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import ij.plugin.DICOM;
import util.MatUtils;
import util.MatViewer;
import util.MongoHelper;

public class Lungs {

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);


    CTStack ctStack = MongoHelper.getDataStore().createQuery(CTStack.class).get();



    List<Mat> mats = new ArrayList<>();
    // TODO need to order by image number found by obtaining info

    for (String path : ctStack.getPaths()) {
      DICOM dicom = new DICOM();
      dicom.open(path);
      mats.add(MatUtils.fromDICOM(dicom));
    }

    new MatViewer(mats).display();
  }

}
