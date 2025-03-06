//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_4;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A collection of assembly measurements
 * 
 * <p>Java class for CuttingItemMeasurementsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CuttingItemMeasurementsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <choice maxOccurs="unbounded">
 *         <element ref="{urn:mtconnect.org:MTConnectAssets:1.4}CommonMeasurement"/>
 *         <element ref="{urn:mtconnect.org:MTConnectAssets:1.4}CuttingItemMeasurement"/>
 *       </choice>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CuttingItemMeasurementsType")
@XmlType(name = "CuttingItemMeasurementsType", propOrder = {
    "commonMeasurementOrCuttingItemMeasurement"
})
public class CuttingItemMeasurementsType {

    @XmlElementRefs({
        @XmlElementRef(name = "CommonMeasurement", namespace = "urn:mtconnect.org:MTConnectAssets:1.4", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CuttingItemMeasurement", namespace = "urn:mtconnect.org:MTConnectAssets:1.4", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<? extends MeasurementType>> commonMeasurementOrCuttingItemMeasurement;

    /**
     * Gets the value of the commonMeasurementOrCuttingItemMeasurement property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the commonMeasurementOrCuttingItemMeasurement property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getCommonMeasurementOrCuttingItemMeasurement().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link CommonMeasurementType }{@code >}
     * {@link JAXBElement }{@code <}{@link CornerRadiusType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingDiameterType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingEdgeLengthType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingHeightType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingItemMeasurementType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingReferencePointType }{@code >}
     * {@link JAXBElement }{@code <}{@link FlangeDiameterType }{@code >}
     * {@link JAXBElement }{@code <}{@link FunctionalLengthType }{@code >}
     * {@link JAXBElement }{@code <}{@link FunctionalWidthType }{@code >}
     * {@link JAXBElement }{@code <}{@link InclinationAngleType }{@code >}
     * {@link JAXBElement }{@code <}{@link IncribedCircleDiameterType }{@code >}
     * {@link JAXBElement }{@code <}{@link PointAngleType }{@code >}
     * {@link JAXBElement }{@code <}{@link ProtrudingLengthType }{@code >}
     * {@link JAXBElement }{@code <}{@link StepDiameterLengthType }{@code >}
     * {@link JAXBElement }{@code <}{@link StepIncludedAngleType }{@code >}
     * {@link JAXBElement }{@code <}{@link ToolCuttingEdgeAngleType }{@code >}
     * {@link JAXBElement }{@code <}{@link ToolLeadAngleType }{@code >}
     * {@link JAXBElement }{@code <}{@link WeightType }{@code >}
     * {@link JAXBElement }{@code <}{@link WiperEdgeLengthType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the commonMeasurementOrCuttingItemMeasurement property.
     */
    public List<JAXBElement<? extends MeasurementType>> getCommonMeasurementOrCuttingItemMeasurement() {
        if (commonMeasurementOrCuttingItemMeasurement == null) {
            commonMeasurementOrCuttingItemMeasurement = new ArrayList<>();
        }
        return this.commonMeasurementOrCuttingItemMeasurement;
    }

}
