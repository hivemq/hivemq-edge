package com.hivemq.edge.modules.adapters.simulation.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;

public class SimulationTagDefinition implements TagDefinition {

    @JsonCreator
    public SimulationTagDefinition() {
    }

}
