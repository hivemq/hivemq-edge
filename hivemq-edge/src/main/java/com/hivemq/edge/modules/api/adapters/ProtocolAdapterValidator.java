package com.hivemq.edge.modules.api.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.api.adapters.model.ProtocolAdapterValidationFailure;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * An object that handles the validation of the configuration bean, ensuring compliance with the requirement
 * schema
 *
 * @author Simon L Johnson
 */
public interface ProtocolAdapterValidator {

    List<ProtocolAdapterValidationFailure> validateConfiguration(@NotNull ObjectMapper objectMapper, @NotNull Object config);

}
