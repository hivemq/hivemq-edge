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
package com.hivemq.bridge;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.common.topic.TopicFilterProcessor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicFilterProcessorTest {

    @Test
    void testReplacement() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "replacement/of/level";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of()).toString();

        assertEquals("replacement/of/level", outTopic);
    }

    @Test
    void testEnvVarEscapeReplacement() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "replacement/$ENV\\{2}/of/level";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of()).toString();

        assertEquals("replacement/$ENV{2}/of/level", outTopic);
    }

    @Test
    void testBrideNameEscapeReplacement() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "replacement/$\\{bridge.name}/of/level";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("replacement/${bridge.name}/of/level", outTopic);
    }

    @Test
    void testBrideNameReplacement() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "replacement/${bridge.name}/of/level";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("replacement/test-bridge/of/level", outTopic);
    }

    @Test
    void testEscapeReplacement() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "replacement/\\{2}/of/level";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("replacement/{2}/of/level", outTopic);
    }

    @Test
    void testSingleTopicReplacements() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{2}/NEU/{4}/allData/{3}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/cell/NEU/machine/allData/department", outTopic);
    }

    @Test
    void testMultipleFollowingTopicsReplacements() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{2-3}/NEU/{1-2}/{4-4}/allData/{1-4}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/cell/department/NEU/site/cell/machine/allData/site/cell/department/machine", outTopic);
    }

    @Test
    void testWildcardTopicReplacements() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{#}/NEU/{#}/allData/";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/site/cell/department/machine/NEU/site/cell/department/machine/allData/", outTopic);
    }

    @Test
    void testStartingAtWildcardTopicReplacements() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{2-#}/NEU/{4-#}/allData/{1-#}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/cell/department/machine/NEU/machine/allData/site/cell/department/machine", outTopic);
    }

    @Test
    void testEnvVarReplacements() {
        final String inTopic = "site/cell/department/machine";
        System.setProperty("EnvTest", "EnvValue");

        final String destinationFilter = "prefix/$ENV{EnvTest}/NEU/$ENV{EnvTest}/allData/";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/EnvValue/NEU/EnvValue/allData/", outTopic);
    }

    //Failure Use Cases
    @Test
    void testEnvVarNotFound() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/$ENV{nothing}/NEU/$ENV{notThere}/allData/";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData/", outTopic);
    }

    //Failure Use Cases
    @Test
    void testLocalVarNotFound() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/${nothing}/NEU/${notThere}/allData/";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData/", outTopic);
    }

    @Test
    void testStartingAtWildcardTooHigh() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{5-#}/NEU/{6-#}/allData/{7-#}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData", outTopic);
    }

    @Test
    void testMultipleFollowingTopicsEndHigherStart() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{3-2}/NEU/{2-1}/{4-1}/allData";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData", outTopic);
    }

    @Test
    void testMultipleFollowingTopicsStartHigherOriginalTopic() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{5-6}/NEU/{6-7}/{7-10}/allData";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData", outTopic);
    }

    @Test
    void testMultipleFollowingTopicsEndHigherOriginalTopic() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{2-6}/NEU/{1-7}/allData/{4-10}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/cell/department/machine/NEU/site/cell/department/machine/allData/machine", outTopic);
    }

    @Test
    void testSingleTopicHigherOriginalTopic() {
        final String inTopic = "site/cell/department/machine";
        final String destinationFilter = "prefix/{5}/NEU/{6}/allData/{7}";

        final String outTopic = TopicFilterProcessor.modifyTopic(destinationFilter, MqttTopic.of(inTopic), Map.of("bridge.name", "test-bridge")).toString();

        assertEquals("prefix/NEU/allData", outTopic);
    }
}
