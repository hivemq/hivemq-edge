//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_2;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * This section provides semantic information for the {{block(ImageFile)}}
 *         entity.
 * 
 * <p>Java class for ImageFilesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ImageFilesType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:2.2}AbstractConfigurationType">
 *       <sequence>
 *         <element name="ImageFile" type="{urn:mtconnect.org:MTConnectDevices:2.2}ImageFileType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ImageFilesType")
@XmlType(name = "ImageFilesType", propOrder = {
    "imageFile"
})
public class ImageFilesType
    extends AbstractConfigurationType
{

    /**
     * reference to a file containing an image of the
     *                 {{block(Component)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ImageFile")
    @XmlElement(name = "ImageFile", required = true)
    protected List<ImageFileType> imageFile;

    /**
     * reference to a file containing an image of the
     *                 {{block(Component)}}.
     * 
     * Gets the value of the imageFile property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the imageFile property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getImageFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ImageFileType }
     * </p>
     * 
     * 
     * @return
     *     The value of the imageFile property.
     */
    public List<ImageFileType> getImageFile() {
        if (imageFile == null) {
            imageFile = new ArrayList<>();
        }
        return this.imageFile;
    }

}
