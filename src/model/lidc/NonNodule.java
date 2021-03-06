//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.29 at 11:43:13 PM GMT 
//


package model.lidc;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}nonNoduleID"/>
 *         &lt;element ref="{}imageZposition"/>
 *         &lt;element ref="{}imageSOP_UID"/>
 *         &lt;element ref="{}locus"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "nonNoduleID",
    "imageZposition",
    "imageSOPUID",
    "locus"
})
@XmlRootElement(name = "nonNodule")
public class NonNodule {

    @XmlElement(required = true)
    protected String nonNoduleID;
    @XmlElement(required = true)
    protected BigDecimal imageZposition;
    @XmlElement(name = "imageSOP_UID", required = true)
    protected String imageSOPUID;
    @XmlElement(required = true)
    protected Locus locus;

    /**
     * Gets the value of the nonNoduleID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNonNoduleID() {
        return nonNoduleID;
    }

    /**
     * Sets the value of the nonNoduleID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNonNoduleID(String value) {
        this.nonNoduleID = value;
    }

    /**
     * Gets the value of the imageZposition property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getImageZposition() {
        return imageZposition;
    }

    /**
     * Sets the value of the imageZposition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setImageZposition(BigDecimal value) {
        this.imageZposition = value;
    }

    /**
     * Gets the value of the imageSOPUID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImageSOPUID() {
        return imageSOPUID;
    }

    /**
     * Sets the value of the imageSOPUID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImageSOPUID(String value) {
        this.imageSOPUID = value;
    }

    /**
     * Gets the value of the locus property.
     * 
     * @return
     *     possible object is
     *     {@link Locus }
     *     
     */
    public Locus getLocus() {
        return locus;
    }

    /**
     * Sets the value of the locus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Locus }
     *     
     */
    public void setLocus(Locus value) {
        this.locus = value;
    }

}
