//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_5;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Statistical operations on data
 * 
 * <p>Java class for DataItemStatisticsTypeEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="DataItemStatisticsTypeEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="AVERAGE"/>
 *     <enumeration value="KURTOSIS"/>
 *     <enumeration value="MAXIMUM"/>
 *     <enumeration value="MEDIAN"/>
 *     <enumeration value="MINIMUM"/>
 *     <enumeration value="MODE"/>
 *     <enumeration value="RANGE"/>
 *     <enumeration value="ROOT_MEAN_SQUARE"/>
 *     <enumeration value="STANDARD_DEVIATION"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemStatisticsTypeEnum")
@XmlType(name = "DataItemStatisticsTypeEnum")
@XmlEnum
public enum DataItemStatisticsTypeEnum {


    /**
     * Mathematical Average value calculated for the data item during the
     *             calculation period.
     * 
     */
    AVERAGE,

    /**
     * A measure of the "peakedness" of a probability
     *             distribution; i.e., the shape of the distribution curve.
     * 
     */
    KURTOSIS,

    /**
     * Maximum value of a data entity or attribute.
     * 
     */
    MAXIMUM,

    /**
     * The middle number of a series of numbers.
     * 
     */
    MEDIAN,

    /**
     * The minimum value of a data entity or attribute.
     * 
     */
    MINIMUM,

    /**
     * The number in a series of numbers that occurs most often.
     * 
     */
    MODE,

    /**
     * Difference between the maximum and minimum value of a data item
     *             during the calculation period. Also represents Peak-to-Peak
     *             measurement in a waveform.
     * 
     */
    RANGE,

    /**
     * Mathematical Root Mean Square (RMS) value calculated for the data
     *             item during the calculation period.
     * 
     */
    ROOT_MEAN_SQUARE,

    /**
     * Statistical Standard Deviation value calculated for the data item
     *             during the calculation period.
     * 
     */
    STANDARD_DEVIATION;

    public String value() {
        return name();
    }

    public static DataItemStatisticsTypeEnum fromValue(String v) {
        return valueOf(v);
    }

}
