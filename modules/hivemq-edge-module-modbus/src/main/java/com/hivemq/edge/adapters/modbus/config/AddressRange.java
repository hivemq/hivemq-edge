package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;

@JsonPropertyOrder({"startIdx", "endIdx"})
public class AddressRange {

    @JsonProperty("startIdx")
    @ModuleConfigField(title = "Start Index",
                       description = "The Starting Index (Incl.) of the Address Range",
                       numberMin = 0,
                       numberMax = ModbusAdapterConfig.PORT_MAX,
                       required = true)
    public final int startIdx;

    @JsonProperty("endIdx")
    @ModuleConfigField(title = "End Index",
                       description = "The Finishing Index (Excl.) of the Address Range",
                       numberMin = 1,
                       numberMax = ModbusAdapterConfig.PORT_MAX,
                       required = true)
    public final int endIdx;

    public AddressRange(
            @JsonProperty(value = "startIdx", required = true) final int startIdx,
            @JsonProperty(value = "endIdx", required = true) final int endIdx) {
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }
}
