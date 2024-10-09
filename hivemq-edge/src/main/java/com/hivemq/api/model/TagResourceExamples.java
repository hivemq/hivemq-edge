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
package com.hivemq.api.model;

public class TagResourceExamples {

    public static final String EXAMPLE_OPC_UA = "{\n" +
            "   \"items\":[\n" +
            "      {\n" +
            "         \"tagAddress\":{\n" +
            "            \"address\":\"ns=2;i=test\"\n" +
            "         },\n" +
            "         \"tag\":\"tag1\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"tagAddress\":{\n" +
            "            \"address\":\"ns=2;i=test2\"\n" +
            "         },\n" +
            "         \"tag\":\"tag2\"\n" +
            "      }\n" +
            "   ]\n" +
            "}";


    public static final String EXAMPLE_ALREADY_PRESENT = "{\n" +
            "   \"type\":\"https://docs.hivemq.com/problem-registry/already-present\",\n" +
            "   \"title\":\"The item already exists.\",\n" +
            "   \"detail\":\"The tag 'tag' cannot be created since another item already exists with the same id.\",\n" +
            "   \"instance\":\"/tags\"\n" +
            "}";

    public static final String EXAMPLE_NOT_PRESENT = "{\n" +
            "   \"type\":\"https://docs.hivemq.com/problem-registry/not-found\",\n" +
            "   \"title\":\"The item cannot be found.\",\n" +
            "   \"detail\":\"The tag 'tag1' cannot be found and therefore cannot be deleted\",\n" +
            "   \"instance\":\"/tags/tag1\"\n" +
            "}";


    public static final String EXAMPLE_LIST = "{\n" +
            "   \"items\":[\n" +
            "      {\n" +
            "         \"tagAddress\":{\n" +
            "            \"address\":\"address1\"\n" +
            "         },\n" +
            "         \"tag\":\"tag1\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"tagAddress\":{\n" +
            "            \"address\":\"address2\"\n" +
            "         },\n" +
            "         \"tag\":\"tag2\"\n" +
            "      }\n" +
            "   ]\n" +
            "}";



}
