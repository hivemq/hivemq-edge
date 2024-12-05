package com.hivemq.api.model.mappings.northbound;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.api.model.JavaScriptConstants;
import com.hivemq.persistence.mappings.NorthboundMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NorthboundMappingModelTest {


    @Test
    void test_constructor_whenInputExpiryIsMaxLong_thenStoredExpiryIsMaxSafeJSValue() {

        final NorthboundMappingModel northboundMappingModel = new NorthboundMappingModel("topic",
                "tag",
                MessageHandlingOptions.MQTTMessagePerSubscription,
                false,
                false,
                List.of(),
                2,
                Long.MAX_VALUE);

        assertEquals(JavaScriptConstants.JS_MAX_SAFE_INTEGER, northboundMappingModel.getMessageExpiryInterval());
    }

    @Test
    void test_to_whenMessageExpiryMaxSafeValue_thenParsedValueIsMaxLong() {

        final NorthboundMappingModel northboundMappingModel = new NorthboundMappingModel("topic",
                "tag",
                MessageHandlingOptions.MQTTMessagePerSubscription,
                false,
                false,
                List.of(),
                2,
                Long.MAX_VALUE);

        assertEquals(JavaScriptConstants.JS_MAX_SAFE_INTEGER, northboundMappingModel.getMessageExpiryInterval());

        final NorthboundMapping northboundMapping = northboundMappingModel.to();
        assertEquals(Long.MAX_VALUE, northboundMapping.getMessageExpiryInterval());

    }


}
