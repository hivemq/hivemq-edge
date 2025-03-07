//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_8;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * The unit vector along which the motion occurs
 * 
 * <p>Java class for MotionAxisType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MotionAxisType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectDevices:1.8>ThreeSpaceValueType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MotionAxisType")
@XmlType(name = "MotionAxisType", propOrder = {
    "value"
})
public class MotionAxisType {

    /**
     * A three dimensional value 'X Y Z' or 'A B C'
     * 
     */
    @XmlValue
    protected List<Float> value;

    /**
     * A three dimensional value 'X Y Z' or 'A B C'
     * 
     * Gets the value of the value property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the value property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Float }
     * </p>
     * 
     * 
     * @return
     *     The value of the value property.
     */
    public List<Float> getValue() {
        if (value == null) {
            value = new ArrayList<>();
        }
        return this.value;
    }

}
