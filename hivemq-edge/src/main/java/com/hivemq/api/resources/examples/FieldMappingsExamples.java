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
