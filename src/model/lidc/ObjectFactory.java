//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.29 at 11:43:13 PM GMT 
//


package model.lidc;

import java.math.BigDecimal;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the model.lidc package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Inclusion_QNAME = new QName("", "inclusion");
    private final static QName _ReadingSessionType_QNAME = new QName("", "readingSessionType");
    private final static QName _ImageSOPUID_QNAME = new QName("", "imageSOP_UID");
    private final static QName _NonNoduleID_QNAME = new QName("", "nonNoduleID");
    private final static QName _ImageZposition_QNAME = new QName("", "imageZposition");
    private final static QName _NoduleID_QNAME = new QName("", "noduleID");
    private final static QName _ServicingRadiologistID_QNAME = new QName("", "servicingRadiologistID");
    private final static QName _AnnotationVersion_QNAME = new QName("", "annotationVersion");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: model.lidc
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Characteristics }
     * 
     */
    public Characteristics createCharacteristics() {
        return new Characteristics();
    }

    /**
     * Create an instance of {@link NonNodule }
     * 
     */
    public NonNodule createNonNodule() {
        return new NonNodule();
    }

    /**
     * Create an instance of {@link Locus }
     * 
     */
    public Locus createLocus() {
        return new Locus();
    }

    /**
     * Create an instance of {@link EdgeMap }
     * 
     */
    public EdgeMap createEdgeMap() {
        return new EdgeMap();
    }

    /**
     * Create an instance of {@link UnblindedReadNodule }
     * 
     */
    public UnblindedReadNodule createUnblindedReadNodule() {
        return new UnblindedReadNodule();
    }

    /**
     * Create an instance of {@link Roi }
     * 
     */
    public Roi createRoi() {
        return new Roi();
    }

    /**
     * Create an instance of {@link ReadingSession }
     * 
     */
    public ReadingSession createReadingSession() {
        return new ReadingSession();
    }

    /**
     * Create an instance of {@link BlindedReadNodule }
     * 
     */
    public BlindedReadNodule createBlindedReadNodule() {
        return new BlindedReadNodule();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "inclusion")
    public JAXBElement<String> createInclusion(String value) {
        return new JAXBElement<String>(_Inclusion_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "readingSessionType")
    public JAXBElement<String> createReadingSessionType(String value) {
        return new JAXBElement<String>(_ReadingSessionType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "imageSOP_UID")
    public JAXBElement<String> createImageSOPUID(String value) {
        return new JAXBElement<String>(_ImageSOPUID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "nonNoduleID")
    public JAXBElement<String> createNonNoduleID(String value) {
        return new JAXBElement<String>(_NonNoduleID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "imageZposition")
    public JAXBElement<BigDecimal> createImageZposition(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_ImageZposition_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "noduleID")
    public JAXBElement<String> createNoduleID(String value) {
        return new JAXBElement<String>(_NoduleID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "servicingRadiologistID")
    public JAXBElement<String> createServicingRadiologistID(String value) {
        return new JAXBElement<String>(_ServicingRadiologistID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "annotationVersion", defaultValue = "3.12")
    public JAXBElement<String> createAnnotationVersion(String value) {
        return new JAXBElement<String>(_AnnotationVersion_QNAME, String.class, null, value);
    }

}
