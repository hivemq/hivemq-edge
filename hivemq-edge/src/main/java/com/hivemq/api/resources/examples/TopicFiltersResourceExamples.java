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
package com.hivemq.api.resources.examples;

public interface TopicFiltersResourceExamples {

    String EXAMPLE_TOPIC_FILTER_LIST = "{\n" +
            "  \"items\" : [ {\n" +
            "    \"topicFilter\" : \"topic1\",\n" +
            "    \"description\" : \"filter1\"\n" +
            "  }, {\n" +
            "    \"topicFilter\" : \"topic2\",\n" +
            "    \"description\" : \"filter2\"\n" +
            "  } ]\n" +
            "}";

    String EXAMPLE_NOT_PRESENT = "{\n" +
            "  \"errors\" : [ {\n" +
            "    \"title\" : \"Resource not found\",\n" +
            "    \"detail\" : \"topic filter with id 'filter1' not found\"\n" +
            "  } ]\n" +
            "}";

    String EXAMPLE_ALREADY_PRESENT = "{\n" +
            "  \"errors\" : [ {\n" +
            "    \"title\" : \"The resource already exists\",\n" +
            "    \"detail\" : \"The topic filter 'filter' cannot be created since another item already exists with the same name.\"\n" +
            "  } ]\n" +
            "}";
}
