package data;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

import model.lidc.LidcReadMessage;
import util.PreTest;

/**
 * @author Stuart Clark
 */
@RunWith(PreTest.class)
public class LidcReadMessageAdapterTest {

  @Test
  public void test() throws Exception {
    String xml = new String(Files.readAllBytes(Paths.get("./testres/read-message.xml")));
    LidcReadMessage readMessage = new LidcReadMessageAdapter().unmarshal(xml);
    assertEquals(4, readMessage.getReadingSessions().size());
  }

}
