package com.hivemq.protocols;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;

public interface InternalPollingContext {

    @NotNull
    FieldMapping getFieldMapping();

}
