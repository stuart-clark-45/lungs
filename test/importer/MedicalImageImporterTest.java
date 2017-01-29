package importer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import model.MedicalImage;
import util.MongoHelper;
import util.PreTest;

/**
 * @author Stuart Clark
 */
@RunWith(PreTest.class)
public class MedicalImageImporterTest {

  @Test
  public void test() throws Exception {
    new MedicalImageImporter().run();

    // Should only be one as only one of the images is a CT scan
    assertEquals(1, MongoHelper.getDataStore().createQuery(MedicalImage.class).count());
  }

}
