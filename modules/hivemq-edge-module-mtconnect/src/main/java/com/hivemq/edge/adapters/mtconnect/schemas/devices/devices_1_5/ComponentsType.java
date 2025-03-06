//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_5;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An XML container that consists of one or more types of component XML
 *         elements.
 * 
 * <p>Java class for ComponentsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element ref="{urn:mtconnect.org:MTConnectDevices:1.5}Component" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentsType")
@XmlType(name = "ComponentsType", propOrder = {
    "component"
})
public class ComponentsType {

    /**
     * An abstract XML element. Replaced in the XML document by types of
     *         component elements representing physical parts and logical functions of
     *         a piece of equipment.
     * 
     */
    @XmlElementRef(name = "Component", namespace = "urn:mtconnect.org:MTConnectDevices:1.5", type = JAXBElement.class)
    protected List<JAXBElement<? extends ComponentType>> component;

    /**
     * An abstract XML element. Replaced in the XML document by types of
     *         component elements representing physical parts and logical functions of
     *         a piece of equipment.
     * 
     * Gets the value of the component property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the component property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getComponent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link ActuatorType }{@code >}
     * {@link JAXBElement }{@code <}{@link AuxiliariesType }{@code >}
     * {@link JAXBElement }{@code <}{@link AxesType }{@code >}
     * {@link JAXBElement }{@code <}{@link BarFeederType }{@code >}
     * {@link JAXBElement }{@code <}{@link ChuckType }{@code >}
     * {@link JAXBElement }{@code <}{@link CommonComponentType }{@code >}
     * {@link JAXBElement }{@code <}{@link ComponentType }{@code >}
     * {@link JAXBElement }{@code <}{@link ControllerType }{@code >}
     * {@link JAXBElement }{@code <}{@link CoolantType }{@code >}
     * {@link JAXBElement }{@code <}{@link DepositionType }{@code >}
     * {@link JAXBElement }{@code <}{@link DeviceType }{@code >}
     * {@link JAXBElement }{@code <}{@link DielectricType }{@code >}
     * {@link JAXBElement }{@code <}{@link DoorType }{@code >}
     * {@link JAXBElement }{@code <}{@link ElectricType }{@code >}
     * {@link JAXBElement }{@code <}{@link EnclosureType }{@code >}
     * {@link JAXBElement }{@code <}{@link EndEffectorType }{@code >}
     * {@link JAXBElement }{@code <}{@link EnvironmentalType }{@code >}
     * {@link JAXBElement }{@code <}{@link FeederType }{@code >}
     * {@link JAXBElement }{@code <}{@link HydraulicType }{@code >}
     * {@link JAXBElement }{@code <}{@link InterfaceType }{@code >}
     * {@link JAXBElement }{@code <}{@link InterfacesType }{@code >}
     * {@link JAXBElement }{@code <}{@link LinearType }{@code >}
     * {@link JAXBElement }{@code <}{@link LoaderType }{@code >}
     * {@link JAXBElement }{@code <}{@link LubricationType }{@code >}
     * {@link JAXBElement }{@code <}{@link MaterialsType }{@code >}
     * {@link JAXBElement }{@code <}{@link PathType }{@code >}
     * {@link JAXBElement }{@code <}{@link PersonnelType }{@code >}
     * {@link JAXBElement }{@code <}{@link PneumaticType }{@code >}
     * {@link JAXBElement }{@code <}{@link PowerType }{@code >}
     * {@link JAXBElement }{@code <}{@link ProcessPowerType }{@code >}
     * {@link JAXBElement }{@code <}{@link ProtectiveType }{@code >}
     * {@link JAXBElement }{@code <}{@link ResourcesType }{@code >}
     * {@link JAXBElement }{@code <}{@link RotaryType }{@code >}
     * {@link JAXBElement }{@code <}{@link SensorType }{@code >}
     * {@link JAXBElement }{@code <}{@link StockType }{@code >}
     * {@link JAXBElement }{@code <}{@link SystemsType }{@code >}
     * {@link JAXBElement }{@code <}{@link ToolingDeliveryType }{@code >}
     * {@link JAXBElement }{@code <}{@link WasteDisposalType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the component property.
     */
    public List<JAXBElement<? extends ComponentType>> getComponent() {
        if (component == null) {
            component = new ArrayList<>();
        }
        return this.component;
    }

}
