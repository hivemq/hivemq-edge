package com.hivemq.validations;

import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.bridge.Bridge;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.configuration.reader.BridgeConfigurator;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public interface BridgeValidator {

    // TODO Refactor Bridge API to reuse this validator
    default void validateBridge(final @NotNull ApiErrorMessages errorMessages, final @NotNull Bridge bridge) {
        ApiErrorUtils.validateRequiredEntity(errorMessages, "bridge", bridge);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridge.getId(), HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateFieldLengthBetweenIncl(errorMessages,
                "id",
                bridge.getId(),
                1,
                HiveMQEdgeConstants.MAX_ID_LEN);

        if (!bridge.getHost().matches(HiveMQEdgeConstants.IPV4_REGEX) &&
                !bridge.getHost().matches(HiveMQEdgeConstants.IPV6_REGEX) &&
                !bridge.getHost().matches(HiveMQEdgeConstants.HOSTNAME_REGEX)) {
            ApiErrorUtils.addValidationError(errorMessages,
                    "host",
                    "Supplied value does not match ipv4, ipv6 or host format");
        }
        ApiErrorUtils.validateFieldValueBetweenIncl(errorMessages,
                "port",
                bridge.getPort(),
                1,
                HiveMQEdgeConstants.MAX_UINT16);

        if(bridge.getLoopPreventionHopCount() != 0){
            ApiErrorUtils.validateFieldValueBetweenIncl(errorMessages,
                    "loopPreventionHopCount",
                    bridge.getLoopPreventionHopCount(),
                    1, 100);
        }

        bridge.getLocalSubscriptions().forEach(s ->
                        validateValidSubscribeTopicField(errorMessages, "local-filters", s.getFilters()));

        bridge.getRemoteSubscriptions().forEach(s ->
                        validateValidSubscribeTopicField(errorMessages, "remote-filters", s.getFilters()));
    }

    private void validateValidSubscribeTopicField(final ApiErrorMessages apiErrorMessages, final String fieldName, final List<String> topicFilters){
        try {
            BridgeConfigurator.validateTopicFilters(fieldName, topicFilters);
        } catch(UnrecoverableException e){
            ApiErrorUtils.addValidationError(apiErrorMessages,
                    fieldName,
                    "Invalid bridge topic filters for subscribing");
        }
    }
}
