package data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import model.CTSlice;
import util.MongoHelper;
import util.Testing;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class CTSliceImporterTest {

  @Test
  public void test() throws Exception {
    new CTSliceImporter().run();

    // Should only be one as only one of the images is a CT scan
    assertEquals(1, MongoHelper.getDataStore().createQuery(CTSlice.class).count());
  }

}
