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

public interface FieldMappingsExamples {


    String ADAPTER_NOT_PRESENT = "{\n" +
            "  \"errors\" : [ {\n" +
            "    \"title\" : \"Resource not found\",\n" +
            "    \"detail\" : \"Adapter with id 'adapter1' not found\"\n" +
            "  } ]\n" +
            "}";

    String FIELD_MAPPINGS_LIST = "{\n" +
            "  \"items\" : [ {\n" +
            "    \"topicFilter\" : \"topic1\",\n" +
            "    \"tag\" : \"someTag\",\n" +
            "    \"fieldMapping\" : [ {\n" +
            "      \"source\" : \"incField1\",\n" +
            "      \"destination\" : \"outField1\",\n" +
            "      \"transformation\" : { }\n" +
            "    }, {\n" +
            "      \"source\" : \"incField2\",\n" +
            "      \"destination\" : \"outField2\",\n" +
            "      \"transformation\" : { }\n" +
            "    }, {\n" +
            "      \"source\" : \"incField3\",\n" +
            "      \"destination\" : \"outField3\",\n" +
            "      \"transformation\" : { }\n" +
            "    } ],\n" +
            "    \"metadata\" : {\n" +
            "      \"source\" : \"\",\n" +
            "      \"destination\" : \"\"\n" +
            "    }\n" +
            "  }, {\n" +
            "    \"topicFilter\" : \"topic2\",\n" +
            "    \"tag\" : \"someTag\",\n" +
            "    \"fieldMapping\" : [ {\n" +
            "      \"source\" : \"incField1\",\n" +
            "      \"destination\" : \"outField1\",\n" +
            "      \"transformation\" : { }\n" +
            "    }, {\n" +
            "      \"source\" : \"incField2\",\n" +
            "      \"destination\" : \"outField2\",\n" +
            "      \"transformation\" : { }\n" +
            "    }, {\n" +
            "      \"source\" : \"incField3\",\n" +
            "      \"destination\" : \"outField3\",\n" +
            "      \"transformation\" : { }\n" +
            "    } ],\n" +
            "    \"metadata\" : {\n" +
            "      \"source\" : \"\",\n" +
            "      \"destination\" : \"\"\n" +
            "    }\n" +
            "  } ]\n" +
            "}";


}
