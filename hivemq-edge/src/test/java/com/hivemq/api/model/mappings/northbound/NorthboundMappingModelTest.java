/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.api.model.mappings.northbound;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.api.model.JavaScriptConstants;
import com.hivemq.api.model.QoSModel;
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
                QoSModel.EXACTLY_ONCE,
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
                QoSModel.EXACTLY_ONCE,
                Long.MAX_VALUE);

        assertEquals(JavaScriptConstants.JS_MAX_SAFE_INTEGER, northboundMappingModel.getMessageExpiryInterval());

        final NorthboundMapping northboundMapping = northboundMappingModel.toPersitence();
        assertEquals(Long.MAX_VALUE, northboundMapping.getMessageExpiryInterval());

    }


}
