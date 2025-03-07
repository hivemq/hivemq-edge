//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * {{block(DataItem)}} describes a piece of information reported about a
 *         piece of equipment.
 * 
 * <p>Java class for DataItemType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Source" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemSourceType" minOccurs="0"/>
 *         <element name="Constraints" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemConstraintsType" minOccurs="0"/>
 *         <element name="Filters" type="{urn:mtconnect.org:MTConnectAssets:1.7}FiltersType" minOccurs="0"/>
 *         <element name="InitialValue" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemNumericValueType" minOccurs="0"/>
 *         <element name="ResetTrigger" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemResetValueType" minOccurs="0"/>
 *         <element name="Definition" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemDefinitionType" minOccurs="0"/>
 *         <element name="Relationships" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemRelationshipsType" minOccurs="0"/>
 *       </all>
 *       <attribute name="name" type="{urn:mtconnect.org:MTConnectAssets:1.7}NameType" />
 *       <attribute name="id" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}IDType" />
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemEnumType" />
 *       <attribute name="subType" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemSubEnumType" />
 *       <attribute name="statistic" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemStatisticsType" />
 *       <attribute name="units" type="{urn:mtconnect.org:MTConnectAssets:1.7}UnitsType" />
 *       <attribute name="nativeUnits" type="{urn:mtconnect.org:MTConnectAssets:1.7}NativeUnitsType" />
 *       <attribute name="nativeScale" type="{urn:mtconnect.org:MTConnectAssets:1.7}NativeScaleType" />
 *       <attribute name="category" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}CategoryType" />
 *       <attribute name="coordinateSystem" type="{urn:mtconnect.org:MTConnectAssets:1.7}CoordinateSystemEnumType" />
 *       <attribute name="coordinateSystemIdRef" type="{urn:mtconnect.org:MTConnectAssets:1.7}CoordinateSystemIdRefType" />
 *       <attribute name="compositionId" type="{urn:mtconnect.org:MTConnectAssets:1.7}CompositionIdType" />
 *       <attribute name="sampleRate" type="{urn:mtconnect.org:MTConnectAssets:1.7}DataItemSampleRateType" />
 *       <attribute name="representation" type="{urn:mtconnect.org:MTConnectAssets:1.7}RepresentationType" default="VALUE" />
 *       <attribute name="significantDigits" type="{urn:mtconnect.org:MTConnectAssets:1.7}SignificantDigitsValueType" />
 *       <attribute name="discrete" type="{urn:mtconnect.org:MTConnectAssets:1.7}DiscreteType" default="false" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemType")
@XmlType(name = "DataItemType", propOrder = {

})
public class DataItemType {

    /**
     * {{block(Source)}} identifies the {{block(Component)}},
     *             {{block(DataItem)}}, or {{block(Composition)}} representing the area
     *             of the piece of equipment from which a measured value originates.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Source")
    @XmlElement(name = "Source")
    protected DataItemSourceType source;
    /**
     * {{block(Constraints)}} {{termplural(organize)}} a set of expected
     *             values that can be reported for this {{block(DataItem)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Constraints")
    @XmlElement(name = "Constraints")
    protected DataItemConstraintsType constraints;
    /**
     * {{block(Filters)}} {{termplural(organize)}} the {{block(Filter)}}
     *             elements associated with this {{block(DataItem)}} element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Filters")
    @XmlElement(name = "Filters")
    protected FiltersType filters;
    /**
     * {{block(InitialValue)}} defines the starting value for a data item
     *             as well as the value to be set for the data item after a reset
     *             event.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "InitialValue")
    @XmlElement(name = "InitialValue")
    protected Float initialValue;
    /**
     * {{block(ResetTrigger)}} identifies the type of event that may cause
     *             a reset to occur.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ResetTrigger")
    @XmlElement(name = "ResetTrigger")
    protected String resetTrigger;
    /**
     * The {{block(Definition)}} defines the meaning of {{block(Entry)}}
     *             and {{block(Cell)}} elements associated with the {{block(DataItem)}}
     *             when the {{property(representation)}} is either {{block(DATA)}} or
     *             {{block(TABLE)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Definition")
    @XmlElement(name = "Definition")
    protected DataItemDefinitionType definition;
    /**
     * {{block(Relationships)}} {{termplural(organize)}} one or more
     *             {{block(DataItemRelationship)}} and
     *             {{block(SpecificationRelationship)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Relationships")
    @XmlElement(name = "Relationships")
    protected DataItemRelationshipsType relationships;
    /**
     * The optional implementors name of the measurement.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    protected String name;
    /**
     * The unique identifier for this element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id")
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    /**
     * The type of measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected String type;
    /**
     * The sub type for the measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subType")
    @XmlAttribute(name = "subType")
    protected String subType;
    /**
     * The statistical operation on this data
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "statistic")
    @XmlAttribute(name = "statistic")
    protected String statistic;
    /**
     * The units of the measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "units")
    @XmlAttribute(name = "units")
    protected String units;
    /**
     * The units as expressed by the machine
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeUnits")
    @XmlAttribute(name = "nativeUnits")
    protected String nativeUnits;
    /**
     * The units as expressed by the machine
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeScale")
    @XmlAttribute(name = "nativeScale")
    protected Float nativeScale;
    /**
     * The category of the data item
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "category")
    @XmlAttribute(name = "category", required = true)
    protected CategoryType category;
    /**
     * A {{block(CoordinateSystem)}} is a reference system that associates a
     *           unique set of n parameters with each point in an n-dimensional space.
     *           {{cite(Ref: ISO 10303-218:2004)}}
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "coordinateSystem")
    @XmlAttribute(name = "coordinateSystem")
    protected CoordinateSystemEnumType coordinateSystem;
    /**
     * The coordinate system reference to a particular coordinate system
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "coordinateSystemIdRef")
    @XmlAttribute(name = "coordinateSystemIdRef")
    @XmlIDREF
    protected Object coordinateSystemIdRef;
    /**
     * The optional composition identifier
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compositionId")
    @XmlAttribute(name = "compositionId")
    @XmlIDREF
    protected Object compositionId;
    /**
     * Used as the default sample rate for waveforms
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleRate")
    @XmlAttribute(name = "sampleRate")
    protected Float sampleRate;
    /**
     * A {{property(representation,DataItem)}} specifies the format and
     *           structure of the information for an {{term(observation)}}. The default
     *           {{property(representation,DataItem)}} is `VALUE` indicating the format
     *           as specified in {{citetitle(MTCPart3)}}. A
     *           {{property(representation,DataItem)}}, other than `VALUE`, will modify
     *           the element name of the {{term(observation)}} by appending the pascal
     *           case of the {{property(representation,DataItem)}} as follows:
     *           \begin{itemize} \item A {{block(DataItem)}} with {{property(type)}}
     *           `TEMPERATURE` and {{property(representation,DataItem)}} of
     *           `TIME_SERIES` becomes `TemperatureTimeSeries` \item **DEPRECATED** A
     *           {{block(DataItem)}} with {{property(type)}} `PART_COUNT` and
     *           {{property(representation,DataItem)}} of `DISCRETE` becomes
     *           `PartCountDiscrete` \item A {{block(DataItem)}} with
     *           {{property(type)}} `VARIABLE` and
     *           {{property(representation,DataItem)}} of `DATA_SET` becomes
     *           `VariableDataSet` \item A {{block(DataItem)}} with {{property(type)}}
     *           `WORK_OFFSET` and {{property(representation,DataItem)}} of `TABLE`
     *           becomes `WorkOffsetTable` \end{itemize} The following constraints
     *           apply to each {{property(representation,DataItem)}}: \begin{itemize}
     *           \item A {{block(DataItem)}} with {{property(representation,DataItem)}}
     *           `TIME_SERIES` **MUST** have a {{property(category,DataItem)}} `SAMPLE`
     *           \item \DEPRECATED A {{block(DataItem)}} with
     *           {{property(representation,DataItem)}} `DISCRETE` **MUST** have a
     *           {{property(category,DataItem)}} `EVENT` \item A {{block(DataItem)}}
     *           with {{property(representation,DataItem)}} `DATA_SET` **MUST** have a
     *           {{property(category,DataItem)}} `EVENT` or `SAMPLE` \item A
     *           {{block(DataItem)}} with {{property(representation,DataItem)}} `TABLE`
     *           **MUST** have a {{property(category,DataItem)}} `EVENT` or `SAMPLE`
     *           \end{itemize} The following sections provide semantic information for
     *           the {{block(Representation)}} model. !Representation.png! Note: See
     *           {{sect(Representation Schema Diagrams)}} for XML schema.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "representation")
    @XmlAttribute(name = "representation")
    protected RepresentationType representation;
    /**
     * The number of significant digits for this data item
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "significantDigits")
    @XmlAttribute(name = "significantDigits")
    protected BigInteger significantDigits;
    /**
     * *MTConnect Version 1.5* replaced {{property(representation)}}
     *           `DISCRETE` with a {{property(discrete)}} attribute for
     *           {{block(DataItem)}}. `DISCRETE` \MUST only be used with a
     *           {{block(DataItem)}} with a {{property(category)}} of `EVENT`. Each
     *           occurrence of the {{term(observation)}} **MAY** have the same value as
     *           the previous occurrence, and **MUST NOT** suppress duplicates.
     *           Examples of `DISCRETE` information as follows: A `PartCount` reporting
     *           the completion of each part using a 1 to indicate completion of a
     *           single part, a `Message` that occurs each time a door opens.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "discrete")
    @XmlAttribute(name = "discrete")
    protected Boolean discrete;

    /**
     * {{block(Source)}} identifies the {{block(Component)}},
     *             {{block(DataItem)}}, or {{block(Composition)}} representing the area
     *             of the piece of equipment from which a measured value originates.
     * 
     * @return
     *     possible object is
     *     {@link DataItemSourceType }
     *     
     */
    public DataItemSourceType getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemSourceType }
     *     
     * @see #getSource()
     */
    public void setSource(DataItemSourceType value) {
        this.source = value;
    }

    /**
     * {{block(Constraints)}} {{termplural(organize)}} a set of expected
     *             values that can be reported for this {{block(DataItem)}}.
     * 
     * @return
     *     possible object is
     *     {@link DataItemConstraintsType }
     *     
     */
    public DataItemConstraintsType getConstraints() {
        return constraints;
    }

    /**
     * Sets the value of the constraints property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemConstraintsType }
     *     
     * @see #getConstraints()
     */
    public void setConstraints(DataItemConstraintsType value) {
        this.constraints = value;
    }

    /**
     * {{block(Filters)}} {{termplural(organize)}} the {{block(Filter)}}
     *             elements associated with this {{block(DataItem)}} element.
     * 
     * @return
     *     possible object is
     *     {@link FiltersType }
     *     
     */
    public FiltersType getFilters() {
        return filters;
    }

    /**
     * Sets the value of the filters property.
     * 
     * @param value
     *     allowed object is
     *     {@link FiltersType }
     *     
     * @see #getFilters()
     */
    public void setFilters(FiltersType value) {
        this.filters = value;
    }

    /**
     * {{block(InitialValue)}} defines the starting value for a data item
     *             as well as the value to be set for the data item after a reset
     *             event.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getInitialValue() {
        return initialValue;
    }

    /**
     * Sets the value of the initialValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getInitialValue()
     */
    public void setInitialValue(Float value) {
        this.initialValue = value;
    }

    /**
     * {{block(ResetTrigger)}} identifies the type of event that may cause
     *             a reset to occur.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResetTrigger() {
        return resetTrigger;
    }

    /**
     * Sets the value of the resetTrigger property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getResetTrigger()
     */
    public void setResetTrigger(String value) {
        this.resetTrigger = value;
    }

    /**
     * The {{block(Definition)}} defines the meaning of {{block(Entry)}}
     *             and {{block(Cell)}} elements associated with the {{block(DataItem)}}
     *             when the {{property(representation)}} is either {{block(DATA)}} or
     *             {{block(TABLE)}}.
     * 
     * @return
     *     possible object is
     *     {@link DataItemDefinitionType }
     *     
     */
    public DataItemDefinitionType getDefinition() {
        return definition;
    }

    /**
     * Sets the value of the definition property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemDefinitionType }
     *     
     * @see #getDefinition()
     */
    public void setDefinition(DataItemDefinitionType value) {
        this.definition = value;
    }

    /**
     * {{block(Relationships)}} {{termplural(organize)}} one or more
     *             {{block(DataItemRelationship)}} and
     *             {{block(SpecificationRelationship)}}.
     * 
     * @return
     *     possible object is
     *     {@link DataItemRelationshipsType }
     *     
     */
    public DataItemRelationshipsType getRelationships() {
        return relationships;
    }

    /**
     * Sets the value of the relationships property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemRelationshipsType }
     *     
     * @see #getRelationships()
     */
    public void setRelationships(DataItemRelationshipsType value) {
        this.relationships = value;
    }

    /**
     * The optional implementors name of the measurement.
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
     * The unique identifier for this element.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getId()
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * The type of measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getType()
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * The sub type for the measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Sets the value of the subType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getSubType()
     */
    public void setSubType(String value) {
        this.subType = value;
    }

    /**
     * The statistical operation on this data
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatistic() {
        return statistic;
    }

    /**
     * Sets the value of the statistic property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getStatistic()
     */
    public void setStatistic(String value) {
        this.statistic = value;
    }

    /**
     * The units of the measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getUnits()
     */
    public void setUnits(String value) {
        this.units = value;
    }

    /**
     * The units as expressed by the machine
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNativeUnits() {
        return nativeUnits;
    }

    /**
     * Sets the value of the nativeUnits property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getNativeUnits()
     */
    public void setNativeUnits(String value) {
        this.nativeUnits = value;
    }

    /**
     * The units as expressed by the machine
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getNativeScale() {
        return nativeScale;
    }

    /**
     * Sets the value of the nativeScale property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getNativeScale()
     */
    public void setNativeScale(Float value) {
        this.nativeScale = value;
    }

    /**
     * The category of the data item
     * 
     * @return
     *     possible object is
     *     {@link CategoryType }
     *     
     */
    public CategoryType getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link CategoryType }
     *     
     * @see #getCategory()
     */
    public void setCategory(CategoryType value) {
        this.category = value;
    }

    /**
     * A {{block(CoordinateSystem)}} is a reference system that associates a
     *           unique set of n parameters with each point in an n-dimensional space.
     *           {{cite(Ref: ISO 10303-218:2004)}}
     * 
     * @return
     *     possible object is
     *     {@link CoordinateSystemEnumType }
     *     
     */
    public CoordinateSystemEnumType getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the value of the coordinateSystem property.
     * 
     * @param value
     *     allowed object is
     *     {@link CoordinateSystemEnumType }
     *     
     * @see #getCoordinateSystem()
     */
    public void setCoordinateSystem(CoordinateSystemEnumType value) {
        this.coordinateSystem = value;
    }

    /**
     * The coordinate system reference to a particular coordinate system
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCoordinateSystemIdRef() {
        return coordinateSystemIdRef;
    }

    /**
     * Sets the value of the coordinateSystemIdRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getCoordinateSystemIdRef()
     */
    public void setCoordinateSystemIdRef(Object value) {
        this.coordinateSystemIdRef = value;
    }

    /**
     * The optional composition identifier
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCompositionId() {
        return compositionId;
    }

    /**
     * Sets the value of the compositionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getCompositionId()
     */
    public void setCompositionId(Object value) {
        this.compositionId = value;
    }

    /**
     * Used as the default sample rate for waveforms
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the value of the sampleRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getSampleRate()
     */
    public void setSampleRate(Float value) {
        this.sampleRate = value;
    }

    /**
     * A {{property(representation,DataItem)}} specifies the format and
     *           structure of the information for an {{term(observation)}}. The default
     *           {{property(representation,DataItem)}} is `VALUE` indicating the format
     *           as specified in {{citetitle(MTCPart3)}}. A
     *           {{property(representation,DataItem)}}, other than `VALUE`, will modify
     *           the element name of the {{term(observation)}} by appending the pascal
     *           case of the {{property(representation,DataItem)}} as follows:
     *           \begin{itemize} \item A {{block(DataItem)}} with {{property(type)}}
     *           `TEMPERATURE` and {{property(representation,DataItem)}} of
     *           `TIME_SERIES` becomes `TemperatureTimeSeries` \item **DEPRECATED** A
     *           {{block(DataItem)}} with {{property(type)}} `PART_COUNT` and
     *           {{property(representation,DataItem)}} of `DISCRETE` becomes
     *           `PartCountDiscrete` \item A {{block(DataItem)}} with
     *           {{property(type)}} `VARIABLE` and
     *           {{property(representation,DataItem)}} of `DATA_SET` becomes
     *           `VariableDataSet` \item A {{block(DataItem)}} with {{property(type)}}
     *           `WORK_OFFSET` and {{property(representation,DataItem)}} of `TABLE`
     *           becomes `WorkOffsetTable` \end{itemize} The following constraints
     *           apply to each {{property(representation,DataItem)}}: \begin{itemize}
     *           \item A {{block(DataItem)}} with {{property(representation,DataItem)}}
     *           `TIME_SERIES` **MUST** have a {{property(category,DataItem)}} `SAMPLE`
     *           \item \DEPRECATED A {{block(DataItem)}} with
     *           {{property(representation,DataItem)}} `DISCRETE` **MUST** have a
     *           {{property(category,DataItem)}} `EVENT` \item A {{block(DataItem)}}
     *           with {{property(representation,DataItem)}} `DATA_SET` **MUST** have a
     *           {{property(category,DataItem)}} `EVENT` or `SAMPLE` \item A
     *           {{block(DataItem)}} with {{property(representation,DataItem)}} `TABLE`
     *           **MUST** have a {{property(category,DataItem)}} `EVENT` or `SAMPLE`
     *           \end{itemize} The following sections provide semantic information for
     *           the {{block(Representation)}} model. !Representation.png! Note: See
     *           {{sect(Representation Schema Diagrams)}} for XML schema.
     * 
     * @return
     *     possible object is
     *     {@link RepresentationType }
     *     
     */
    public RepresentationType getRepresentation() {
        if (representation == null) {
            return RepresentationType.VALUE;
        } else {
            return representation;
        }
    }

    /**
     * Sets the value of the representation property.
     * 
     * @param value
     *     allowed object is
     *     {@link RepresentationType }
     *     
     * @see #getRepresentation()
     */
    public void setRepresentation(RepresentationType value) {
        this.representation = value;
    }

    /**
     * The number of significant digits for this data item
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSignificantDigits() {
        return significantDigits;
    }

    /**
     * Sets the value of the significantDigits property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getSignificantDigits()
     */
    public void setSignificantDigits(BigInteger value) {
        this.significantDigits = value;
    }

    /**
     * *MTConnect Version 1.5* replaced {{property(representation)}}
     *           `DISCRETE` with a {{property(discrete)}} attribute for
     *           {{block(DataItem)}}. `DISCRETE` \MUST only be used with a
     *           {{block(DataItem)}} with a {{property(category)}} of `EVENT`. Each
     *           occurrence of the {{term(observation)}} **MAY** have the same value as
     *           the previous occurrence, and **MUST NOT** suppress duplicates.
     *           Examples of `DISCRETE` information as follows: A `PartCount` reporting
     *           the completion of each part using a 1 to indicate completion of a
     *           single part, a `Message` that occurs each time a door opens.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDiscrete() {
        if (discrete == null) {
            return false;
        } else {
            return discrete;
        }
    }

    /**
     * Sets the value of the discrete property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     * @see #isDiscrete()
     */
    public void setDiscrete(Boolean value) {
        this.discrete = value;
    }

}
