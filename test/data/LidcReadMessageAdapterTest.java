package data;

import model.lidc.LidcReadMessage;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Stuart Clark
 */
public class LidcReadMessageAdapterTest {

  @Test
  public void test() throws Exception {
    String xml = new String(Files.readAllBytes(Paths.get("./testres/read-message.xml")));
    LidcReadMessage readMessage = new LidcReadMessageAdapter().unmarshal(xml);


  }

}
