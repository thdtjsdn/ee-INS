//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.06.08 at 04:37:43 PM CEST 
//


package dk.frv.enav.ins.route.monalisa.se.sspa.optiroute;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * Data on the ship that the route will be optimized for
 * 
 * <p>Java class for CurrentShipDataType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CurrentShipDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="mmsi" type="{http://www.sspa.se/optiroute}MMSIType" minOccurs="0"/>
 *         &lt;element name="imoid" type="{http://www.sspa.se/optiroute}IMOIDType" minOccurs="0"/>
 *         &lt;element name="forwardtrim" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="afttrim" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CurrentShipDataType", propOrder = {
    "mmsi",
    "imoid",
    "forwardtrim",
    "afttrim"
})
public class CurrentShipDataType {

    protected String mmsi;
    protected String imoid;
    protected float forwardtrim;
    protected float afttrim;

    /**
     * Gets the value of the mmsi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMmsi() {
        return mmsi;
    }

    /**
     * Sets the value of the mmsi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMmsi(String value) {
        this.mmsi = value;
    }

    /**
     * Gets the value of the imoid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImoid() {
        return imoid;
    }

    /**
     * Sets the value of the imoid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImoid(String value) {
        this.imoid = value;
    }

    /**
     * Gets the value of the forwardtrim property.
     * 
     */
    public float getForwardtrim() {
        return forwardtrim;
    }

    /**
     * Sets the value of the forwardtrim property.
     * 
     */
    public void setForwardtrim(float value) {
        this.forwardtrim = value;
    }

    /**
     * Gets the value of the afttrim property.
     * 
     */
    public float getAfttrim() {
        return afttrim;
    }

    /**
     * Sets the value of the afttrim property.
     * 
     */
    public void setAfttrim(float value) {
        this.afttrim = value;
    }

}
