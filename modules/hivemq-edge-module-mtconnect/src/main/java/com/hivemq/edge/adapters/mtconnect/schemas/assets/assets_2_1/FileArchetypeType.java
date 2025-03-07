//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(AbstractFile)}} type that provides information common to all
 *         versions of a file.
 * 
 * <p>Java class for FileArchetypeType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="FileArchetypeType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectAssets:2.1}AssetType">
 *       <all>
 *         <element name="FileProperties" type="{urn:mtconnect.org:MTConnectAssets:2.1}FilePropertiesType" minOccurs="0"/>
 *         <element name="FileComments" type="{urn:mtconnect.org:MTConnectAssets:2.1}FileCommentsType" minOccurs="0"/>
 *       </all>
 *       <attribute name="name" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.1}FileNameType" />
 *       <attribute name="mediaType" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.1}FileMimeTypeType" />
 *       <attribute name="applicationCategory" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.1}ApplicationCategoryType" />
 *       <attribute name="applicationType" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.1}ApplicationTypeType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "FileArchetypeType")
@XmlType(name = "FileArchetypeType", propOrder = {
    "fileProperties",
    "fileComments"
})
public class FileArchetypeType
    extends AssetType
{

    /**
     * {{block(FileProperties)}} groups one or more
     *                 {{block(FileProperty)}} entities for a {{block(File)}}. See
     *                 {{sect(FileProperty)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "FileProperties")
    @XmlElement(name = "FileProperties")
    protected FilePropertiesType fileProperties;
    /**
     * {{block(FileComments)}} groups one or more
     *                 {{block(FileComment)}} entities for a {{block(File)}}. See
     *                 {{sect(FileComment)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "FileComments")
    @XmlElement(name = "FileComments")
    protected FileCommentsType fileComments;
    /**
     * The file name
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name", required = true)
    protected String name;
    /**
     * The mime type of the file
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "mediaType")
    @XmlAttribute(name = "mediaType", required = true)
    protected String mediaType;
    /**
     * The classification of this file
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "applicationCategory")
    @XmlAttribute(name = "applicationCategory", required = true)
    protected String applicationCategory;
    /**
     * The sub classification of this file
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "applicationType")
    @XmlAttribute(name = "applicationType", required = true)
    protected String applicationType;

    /**
     * {{block(FileProperties)}} groups one or more
     *                 {{block(FileProperty)}} entities for a {{block(File)}}. See
     *                 {{sect(FileProperty)}}.
     * 
     * @return
     *     possible object is
     *     {@link FilePropertiesType }
     *     
     */
    public FilePropertiesType getFileProperties() {
        return fileProperties;
    }

    /**
     * Sets the value of the fileProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link FilePropertiesType }
     *     
     * @see #getFileProperties()
     */
    public void setFileProperties(FilePropertiesType value) {
        this.fileProperties = value;
    }

    /**
     * {{block(FileComments)}} groups one or more
     *                 {{block(FileComment)}} entities for a {{block(File)}}. See
     *                 {{sect(FileComment)}}.
     * 
     * @return
     *     possible object is
     *     {@link FileCommentsType }
     *     
     */
    public FileCommentsType getFileComments() {
        return fileComments;
    }

    /**
     * Sets the value of the fileComments property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileCommentsType }
     *     
     * @see #getFileComments()
     */
    public void setFileComments(FileCommentsType value) {
        this.fileComments = value;
    }

    /**
     * The file name
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getName()
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * The mime type of the file
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Sets the value of the mediaType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getMediaType()
     */
    public void setMediaType(String value) {
        this.mediaType = value;
    }

    /**
     * The classification of this file
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationCategory() {
        return applicationCategory;
    }

    /**
     * Sets the value of the applicationCategory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getApplicationCategory()
     */
    public void setApplicationCategory(String value) {
        this.applicationCategory = value;
    }

    /**
     * The sub classification of this file
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationType() {
        return applicationType;
    }

    /**
     * Sets the value of the applicationType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getApplicationType()
     */
    public void setApplicationType(String value) {
        this.applicationType = value;
    }

}
