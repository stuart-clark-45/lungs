package core;

import generator.CTStackGenerator;
import importer.MedicalImageImporter;

/**
 * Used to import and aggregate all of the data required for the system to be run.
 *
 * @author Stuart Clark
 */
public class DataPipeline {

  public static void main(String[] args) {
    new MedicalImageImporter().run();
    new CTStackGenerator().run();
  }

}
