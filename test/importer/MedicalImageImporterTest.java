package importer;

import org.junit.Test;
import org.junit.runner.RunWith;

import util.PreTest;

/**
 * @author Stuart Clark
 */
@RunWith(PreTest.class)
public class MedicalImageImporterTest {

  @Test
  public void test() throws Exception {
    new MedicalImageImporter().call();
  }

}
