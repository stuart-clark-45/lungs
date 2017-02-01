package data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import model.lidc.LidcReadMessage;
import model.lidc.ReadingSession;

/**
 * Used to unmarshal {@link LidcReadMessage}s.
 *
 * @author Stuart Clark
 */
public class LidcReadMessageAdapter extends XmlAdapter<String, LidcReadMessage> {

  @Override
  public LidcReadMessage unmarshal(String v) throws Exception {
    // Init the unmarshaller
    JAXBContext jaxb = JAXBContext.newInstance(ReadingSession.class);
    Unmarshaller unmarshaller = jaxb.createUnmarshaller();

    // Parse the document
    InputStream is = new ByteArrayInputStream(v.getBytes(StandardCharsets.UTF_8));
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(is);

    // Create LidcReadMessage
    LidcReadMessage readMessage = new LidcReadMessage();
    List<ReadingSession> sessions = readMessage.getReadingSessions();

    // Populate sessions list
    NodeList children = doc.getFirstChild().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if (child.getNodeName().equals("readingSession")) {
        String nodeAsString = nodeToString(child);
        sessions.add((ReadingSession) unmarshaller.unmarshal(new StringReader(nodeAsString)));
      }
      
    }

    return readMessage;
  }

  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.setOutputProperty(OutputKeys.INDENT, "yes");
    t.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }

  @Override
  public String marshal(LidcReadMessage v) throws Exception {
    throw new UnsupportedOperationException(
        "Marshalling of LidcReadMessages has not been implemented");
  }
}
