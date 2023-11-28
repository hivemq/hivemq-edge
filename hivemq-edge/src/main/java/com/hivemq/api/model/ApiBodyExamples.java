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

/**
 *
 * //TODO pad this out with real examples
 * @author Simon L Johnson
 */
public interface ApiBodyExamples {

    String EXAMPLE_HEALTH_LIVENESS = "{\n" + "    \"status\": \"UP\"\n" + "}";
    String EXAMPLE_HEALTH_READINESS = "{\n" + "    \"status\": \"UP\"\n" + "}";
    String EXAMPLE_LISTENER_LIST_JSON = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"name\": \"tcp-listener-1883\",\n" +
            "            \"hostName\": \"localhost\",\n" +
            "            \"port\": 1883\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"udp-listener-2442\",\n" +
            "            \"hostName\": \"localhost\",\n" +
            "            \"port\": 2442\n" +
            "        }\n" +
            "    ]\n" +
            "}";
    String EXAMPLE_XML_EXPORT_ERROR_JSON = "{\n" +
            "    \"message\": \"An unknown error occurred processing your request\",\n" +
            "    \"cause\": \"xml export not allowed\"\n" +
            "}";
    String EXAMPLE_NOTIFICATION_JSON = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"level\": \"WARNING\",\n" +
            "            \"title\": \"Default Credentials Need Changing!\",\n" +
            "            \"description\": \"Your gateway access is configured to use the default username/password combination. This is a security risk. Please ensure you modify your access credentials in your configuration.xml file.\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    String EXAMPLE_CONFIGURATION_JSON = "{\n" +
            "    \"environment\": {\n" +
            "        \"properties\": {\n" +
            "            \"environment-type\": \"TEST\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"cloudLink\": {\n" +
            "        \"displayText\": \"HiveMQ Cloud\",\n" +
            "        \"url\": \"https://hivemq.com/cloud\",\n" +
            "        \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "        \"external\": true\n" +
            "    },\n" +
            "    \"gitHubLink\": {\n" +
            "        \"displayText\": \"GitHub\",\n" +
            "        \"url\": \"https://github.com/hivemq/hivemq-edge\",\n" +
            "        \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "        \"external\": true\n" +
            "    },\n" +
            "    \"documentationLink\": {\n" +
            "        \"displayText\": \"Documentation\",\n" +
            "        \"url\": \"https://github.com/hivemq/hivemq-edge/README.MD\",\n" +
            "        \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "        \"external\": true\n" +
            "    },\n" +
            "    \"firstUseInformation\": {\n" +
            "        \"firstUse\": false,\n" +
            "        \"prefillUsername\": \"admin\",\n" +
            "        \"prefillPassword\": \"password\",\n" +
            "        \"firstUseTitle\": \"Welcome To HiveMQ Edge\",\n" +
            "        \"firstUseDescription\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\"\n" +
            "    },\n" +
            "    \"ctas\": {\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"displayText\": \"Connect My First Device\",\n" +
            "                \"url\": \"./protocol-adapters?from=dashboard-cta\",\n" +
            "                \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "                \"external\": false\n" +
            "            },\n" +
            "            {\n" +
            "                \"displayText\": \"Connect To My MQTT Broker\",\n" +
            "                \"url\": \"./bridges?from=dashboard-cta\",\n" +
            "                \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "                \"external\": false\n" +
            "            },\n" +
            "            {\n" +
            "                \"displayText\": \"Learn More\",\n" +
            "                \"url\": \"resources?from=dashboard-cta\",\n" +
            "                \"description\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,\",\n" +
            "                \"external\": false\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"resources\": {\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"displayText\": \"Power Of Smart Manufacturing\",\n" +
            "                \"url\": \"https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/\",\n" +
            "                \"description\": \"\",\n" +
            "                \"target\": \"\",\n" +
            "                \"imageUrl\": \"\",\n" +
            "                \"external\": true\n" +
            "            },\n" +
            "            {\n" +
            "                \"displayText\": \"Power Of Smart Manufacturing\",\n" +
            "                \"url\": \"https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/\",\n" +
            "                \"description\": \"\",\n" +
            "                \"target\": \"\",\n" +
            "                \"imageUrl\": \"\",\n" +
            "                \"external\": true\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"modules\": {\n" +
            "        \"items\": []\n" +
            "    },\n" +
            "    \"extensions\": {\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"id\": \"extension-1\",\n" +
            "                \"version\": \"1.0.0\",\n" +
            "                \"name\": \"My First Extension\",\n" +
            "                \"description\": \"Some extension description here which could span multiple lines\",\n" +
            "                \"author\": \"HiveMQ\",\n" +
            "                \"priority\": 0\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"hivemq-allow-all-extension\",\n" +
            "                \"version\": \"1.0.0\",\n" +
            "                \"name\": \"Allow All Extension\",\n" +
            "                \"author\": \"HiveMQ\",\n" +
            "                \"priority\": 0,\n" +
            "                \"installed\": true\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    String EXAMPLE_CAPABILITIES_JSON =
            "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"test-capability\"\n" +
            "            \"displayName\": \"Super useful Capability\"\n" +
            "            \"description\": \"This capability is really useful for so many reasons.\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    String EXAMPLE_CONNECTION_STATUS_LIST_JSON = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"status\": \"CONNECTED\",\n" +
            "            \"id\": \"cloud\",\n" +
            "            \"type\": \"bridge\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";


    //-- Authentication
    String EXAMPLE_AUTHENTICATION_JSON = "{\n" +
            "    \"token\": \"eyJraWQiOiIwMDAwMSIsImFsZyI6IlJTMjU2In0.eyJqdGkiOiJpb09YbmdWQW1ncl9rSGxZMlRPNWx3IiwiaWF0IjoxNjg3OTQ2MzkwLCJhdWQiOiJIaXZlTVEtRWRnZS1BcGkiLCJpc3MiOiJIaXZlTVEtRWRnZSIsImV4cCI6MTY4Nzk0ODE5MCwibmJmIjoxNjg3OTQ2MjcwLCJzdWIiOiJhZG1pbiIsInJvbGVzIjpbImFkbWluIl19.F4fCJcLobUJXR8rcER_sXVR2l6LhGc6LrnpDlBfuCmVQI22UjLjh-GBYPJV_VF17at_ChBS0UePN9dF4U0i5SsuLcLbrl6QMyI3kmiDxvZCKPWPJGJfiqljVysbQS5vK2F8eJmVFWr0Bb5rXjTtClLIfDGTLEoETbUOMfmic5EzPdWwLN7i3NbuE3xl9u0RepJwVNf0eZrvwIQjpeLZ8vNx9eIVUeMhXpylrQGlDeikJn_F6K89hc1igl2hzN4aU9oT-WOLeQ82oRq7IhL1Rzi1K9NdKMS_xrpV951basq_419oyGyQ6zcxORyC7vsGLZPGi0sHsSJdQ-j12xhPsMg\"\n" +
            "}";
    String EXAMPLE_AUTHENTICATION_ERROR_JSON = "{\n" + "    \"title\": \"Invalid username and/or password\"\n" + "}";

    //-- Connection Statis
    String EXAMPLE_CONNECTION_STATUS_JSON = "{\n" +
            "    \"status\": \"CONNECTED\",\n" +
            "    \"id\": \"cloud\",\n" +
            "    \"type\": \"bridge\"\n" +
            "}";

    //-- Bridge
    String EXAMPLE_BRIDGE_JSON = "{\n" +
            "    \"id\": \"cloud\",\n" +
            "    \"host\": \"REDACTED.cloud\",\n" +
            "    \"port\": 8883,\n" +
            "    \"clientId\": \"cloud\",\n" +
            "    \"keepAlive\": 60,\n" +
            "    \"sessionExpiry\": 3600,\n" +
            "    \"cleanStart\": false,\n" +
            "    \"username\": \"username\",\n" +
            "    \"password\": \"password\",\n" +
            "    \"loopPreventionEnabled\": true,\n" +
            "    \"loopPreventionHopCount\": 1,\n" +
            "    \"remoteSubscriptions\": [],\n" +
            "    \"localSubscriptions\": [\n" +
            "        {\n" +
            "            \"filters\": [\n" +
            "                \"#\"\n" +
            "            ],\n" +
            "            \"destination\": \"prefix/{#}/bridge/${bridge.name}\",\n" +
            "            \"excludes\": [],\n" +
            "            \"customUserProperties\": [\n" +
            "                {\n" +
            "                    \"key\": \"test1\",\n" +
            "                    \"value\": \"test2\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"preserveRetain\": true,\n" +
            "            \"maxQoS\": 0\n" +
            "        }\n" +
            "    ],\n" +
            "    \"tlsConfiguration\": {\n" +
            "        \"enabled\": true,\n" +
            "        \"keystorePassword\": \"\",\n" +
            "        \"privateKeyPassword\": \"\",\n" +
            "        \"truststorePassword\": \"\",\n" +
            "        \"protocols\": [],\n" +
            "        \"cipherSuites\": [],\n" +
            "        \"keystoreType\": \"JKS\",\n" +
            "        \"truststoreType\": \"JKS\",\n" +
            "        \"verifyHostname\": true,\n" +
            "        \"handshakeTimeout\": 10\n" +
            "    },\n" +
            "    \"bridgeRuntimeInformation\": {\n" +
            "        \"connectionStatus\": {\n" +
            "            \"status\": \"CONNECTED\",\n" +
            "            \"id\": \"simons-cloud\",\n" +
            "            \"type\": \"bridge\"\n" +
            "        }\n" +
            "    }\n" +
            "}";


    String EXAMPLE_BRIDGE_LIST_JSON = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"cloud\",\n" +
            "            \"host\": \"REDACTED.cloud\",\n" +
            "            \"port\": 8883,\n" +
            "            \"clientId\": \"cloud\",\n" +
            "            \"keepAlive\": 60,\n" +
            "            \"sessionExpiry\": 3600,\n" +
            "            \"cleanStart\": false,\n" +
            "            \"username\": \"username\",\n" +
            "            \"password\": \"*****\",\n" +
            "            \"loopPreventionEnabled\": true,\n" +
            "            \"loopPreventionHopCount\": 1,\n" +
            "            \"remoteSubscriptions\": [],\n" +
            "            \"localSubscriptions\": [\n" +
            "                {\n" +
            "                    \"filters\": [\n" +
            "                        \"#\"\n" +
            "                    ],\n" +
            "                    \"destination\": \"prefix/{#}/bridge/${bridge.name}\",\n" +
            "                    \"excludes\": [],\n" +
            "                    \"customUserProperties\": [\n" +
            "                        {\n" +
            "                            \"key\": \"test1\",\n" +
            "                            \"value\": \"test2\"\n" +
            "                        }\n" +
            "                    ],\n" +
            "                    \"preserveRetain\": true,\n" +
            "                    \"maxQoS\": 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"tlsConfiguration\": {\n" +
            "                \"enabled\": true,\n" +
            "                \"keystorePassword\": \"\",\n" +
            "                \"privateKeyPassword\": \"\",\n" +
            "                \"truststorePassword\": \"\",\n" +
            "                \"protocols\": [],\n" +
            "                \"cipherSuites\": [],\n" +
            "                \"keystoreType\": \"JKS\",\n" +
            "                \"truststoreType\": \"JKS\",\n" +
            "                \"verifyHostname\": true,\n" +
            "                \"handshakeTimeout\": 10\n" +
            "            },\n" +
            "            \"bridgeRuntimeInformation\": {\n" +
            "                \"connectionStatus\": {\n" +
            "                    \"status\": \"CONNECTED\",\n" +
            "                    \"id\": \"cloud\",\n" +
            "                    \"type\": \"bridge\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    //-- Metrics
    String EXAMPLE_METRIC_LIST_JSON = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.local.publish.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"simulation\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.dropped.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.mqtt.connection.not-writable.current\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.forward.publish.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.local.publish.received.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.outgoing.publish.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.sessions.overall.current\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.forward.publish.failed.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.networking.bytes.read.total\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.outgoing.total.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.messages.governance.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.local.publish.failed.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.networking.connections.current\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.persistence.retained-messages.in-memory.total-size\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.forward.publish.loop-hops-exceeded.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.incoming.connect.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.local.publish.no-subscriber-present.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.incoming.publish.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.incoming.total.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.will.count.current\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.will.published.count.total\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.persistence.client-session.subscriptions.in-memory.total-size\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.remote.publish.loop-hops-exceeded.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.networking.bytes.write.total\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.forward.publish.excluded.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.networking.connections-closed.total.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.bridge.simons-cloud.remote.publish.received.count\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.subscriptions.overall.current\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.persistence.queued-messages.in-memory.total-size\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.persistence.client-sessions.in-memory.total-size\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"com.hivemq.edge.messages.retained.current\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    //-- Search API
    String EXAMPLE_DATAPOINT_JSON = "{\n" +
            "    \"sampleTime\": \"2023-06-28T11:39:12.789+01\",\n" +
            "    \"value\": 0\n" +
            "}";

    String EXAMPLE_ADAPTER = "{\n" +
            "    \"id\": \"test-simulation-server\",\n" +
            "    \"type\": \"simulation\",\n" +
            "    \"config\": {\n" +
            "        \"id\": \"test-simulation-server\",\n" +
            "        \"port\": 5021,\n" +
            "        \"host\": \"127.0.0.1\",\n" +
            "        \"pollingIntervalMillis\": 1000,\n" +
            "        \"subscriptions\": [\n" +
            "            {\n" +
            "                \"filter\": \"my-simulation-server/my-simulation-path-100\",\n" +
            "                \"destination\": \"test\",\n" +
            "                \"qos\": 0\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"adapterRuntimeInformation\": {\n" +
            "        \"lastStartedAttemptTime\": \"2023-06-28T10:57:18.707+01\",\n" +
            "        \"numberOfDaemonProcesses\": 1,\n" +
            "        \"connectionStatus\": {\n" +
            "            \"status\": \"CONNECTED\",\n" +
            "            \"id\": \"test-simulation-server\",\n" +
            "            \"type\": \"adapter\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    String EXAMPLE_ADAPTER_LIST = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"test-simulation-server\",\n" +
            "            \"type\": \"simulation\",\n" +
            "            \"config\": {\n" +
            "                \"id\": \"test-simulation-server\",\n" +
            "                \"port\": 5021,\n" +
            "                \"host\": \"127.0.0.1\",\n" +
            "                \"pollingIntervalMillis\": 1000,\n" +
            "                \"subscriptions\": [\n" +
            "                    {\n" +
            "                        \"filter\": \"my-simulation-server/my-simulation-path-100\",\n" +
            "                        \"destination\": \"test\",\n" +
            "                        \"qos\": 0\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"adapterRuntimeInformation\": {\n" +
            "                \"lastStartedAttemptTime\": \"2023-06-28T10:57:18.707+01\",\n" +
            "                \"numberOfDaemonProcesses\": 1,\n" +
            "                \"connectionStatus\": {\n" +
            "                    \"status\": \"CONNECTED\",\n" +
            "                    \"id\": \"test-simulation-server\",\n" +
            "                    \"type\": \"adapter\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    String EXAMPLE_FILTERED_TYPE_ADAPTERS = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"test-simulation-server\",\n" +
            "            \"type\": \"simulation\",\n" +
            "            \"config\": {\n" +
            "                \"id\": \"test-simulation-server\",\n" +
            "                \"port\": 5021,\n" +
            "                \"host\": \"127.0.0.1\",\n" +
            "                \"pollingIntervalMillis\": 1000,\n" +
            "                \"subscriptions\": [\n" +
            "                    {\n" +
            "                        \"filter\": \"my-simulation-server/my-simulation-path-100\",\n" +
            "                        \"destination\": \"test\",\n" +
            "                        \"qos\": 0\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"adapterRuntimeInformation\": {\n" +
            "                \"lastStartedAttemptTime\": \"2023-06-28T10:57:18.707+01\",\n" +
            "                \"numberOfDaemonProcesses\": 1,\n" +
            "                \"connectionStatus\": {\n" +
            "                    \"status\": \"CONNECTED\",\n" +
            "                    \"id\": \"test-simulation-server\",\n" +
            "                    \"type\": \"adapter\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    String EXAMPLE_ADAPTER_TYPE_LIST = "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"modbus\",\n" +
            "            \"protocol\": \"ModBus TCP\",\n" +
            "            \"name\": \"ModBus to MQTT Protocol Adapter\",\n" +
            "            \"description\": \"Connects existing ModBus services to MQTT.\",\n" +
            "            \"url\": \"https://www.hivemq.com/edge/modbus/\",\n" +
            "            \"version\": \"1.0.0\",\n" +
            "            \"logoUrl\": \"modbus_logo.png\",\n" +
            "            \"author\": \"HiveMQ\",\n" +
            "            \"configSchema\": {\n" +
            "                \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "                \"$defs\": {\n" +
            "                    \"AddressRange\": {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"endIdx\": {\n" +
            "                                \"type\": \"integer\",\n" +
            "                                \"title\": \"End Index\",\n" +
            "                                \"description\": \"The Finishing Index (Incl.) of the Address Range\",\n" +
            "                                \"default\": 10,\n" +
            "                                \"minimum\": 1.0,\n" +
            "                                \"maximum\": 65535.0\n" +
            "                            },\n" +
            "                            \"startIdx\": {\n" +
            "                                \"type\": \"integer\",\n" +
            "                                \"title\": \"Start Index\",\n" +
            "                                \"description\": \"The Starting Index (Incl.) of the Address Range\",\n" +
            "                                \"default\": 1,\n" +
            "                                \"minimum\": 1.0,\n" +
            "                                \"maximum\": 65535.0\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                \"type\": \"object\",\n" +
            "                \"properties\": {\n" +
            "                    \"host\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"Host\",\n" +
            "                        \"description\": \"host to connect to\",\n" +
            "                        \"format\": \"hostname\"\n" +
            "                    },\n" +
            "                    \"id\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"Identifier\",\n" +
            "                        \"description\": \"Unique identifier for this protocol adapter\",\n" +
            "                        \"minLength\": 1,\n" +
            "                        \"maxLength\": 1024,\n" +
            "                        \"format\": \"identifier\",\n" +
            "                        \"pattern\": \"([a-zA-Z_0-9\\\\-])*\"\n" +
            "                    },\n" +
            "                    \"maxPollingErrorsBeforeRemoval\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"title\": \"Max. Polling Errors\",\n" +
            "                        \"description\": \"Max Errors Polling The Endpoint Before The Polling Deamon Is Stopped\",\n" +
            "                        \"default\": 10,\n" +
            "                        \"minimum\": 3.0\n" +
            "                    },\n" +
            "                    \"port\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"title\": \"Port\",\n" +
            "                        \"description\": \"Port to connect to\",\n" +
            "                        \"minimum\": 1.0,\n" +
            "                        \"maximum\": 65535.0\n" +
            "                    },\n" +
            "                    \"publishChangedDataOnly\": {\n" +
            "                        \"type\": \"boolean\",\n" +
            "                        \"title\": \"Publish Only Changed Data\",\n" +
            "                        \"description\": \"Only Publish Data That Has Changed Since Last DataPoint\",\n" +
            "                        \"default\": true,\n" +
            "                        \"format\": \"boolean\"\n" +
            "                    },\n" +
            "                    \"publishingInterval\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"title\": \"Publishing interval [ms]\",\n" +
            "                        \"description\": \"Publishing interval in milliseconds for this subscription on the server\",\n" +
            "                        \"default\": 1000,\n" +
            "                        \"minimum\": 1.0\n" +
            "                    },\n" +
            "                    \"subscriptions\": {\n" +
            "                        \"title\": \"Subscriptions\",\n" +
            "                        \"description\": \"Map your sensor data to MQTT Topics\",\n" +
            "                        \"type\": \"array\",\n" +
            "                        \"items\": {\n" +
            "                            \"type\": \"object\",\n" +
            "                            \"properties\": {\n" +
            "                                \"coils\": {\n" +
            "                                    \"$ref\": \"#/$defs/AddressRange\"\n" +
            "                                },\n" +
            "                                \"destination\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"title\": \"Destination MQTT topic\",\n" +
            "                                    \"description\": \"The MQTT topic to publish to\",\n" +
            "                                    \"format\": \"mqtt-topic\"\n" +
            "                                },\n" +
            "                                \"holding-registers\": {\n" +
            "                                    \"$ref\": \"#/$defs/AddressRange\"\n" +
            "                                },\n" +
            "                                \"input-registers\": {\n" +
            "                                    \"$ref\": \"#/$defs/AddressRange\"\n" +
            "                                },\n" +
            "                                \"qos\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"MQTT QoS\",\n" +
            "                                    \"description\": \"MQTT quality of service level\",\n" +
            "                                    \"default\": 0,\n" +
            "                                    \"minimum\": 0.0,\n" +
            "                                    \"maximum\": 2.0\n" +
            "                                }\n" +
            "                            },\n" +
            "                            \"required\": [\n" +
            "                                \"destination\"\n" +
            "                            ],\n" +
            "                            \"title\": \"Subscriptions\",\n" +
            "                            \"description\": \"Map your sensor data to MQTT Topics\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                \"required\": [\n" +
            "                    \"host\",\n" +
            "                    \"id\",\n" +
            "                    \"port\",\n" +
            "                    \"publishingInterval\"\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"simulation\",\n" +
            "            \"protocol\": \"Simulation\",\n" +
            "            \"name\": \"Simulated Edge Device\",\n" +
            "            \"description\": \"Simulates traffic from an edge device.\",\n" +
            "            \"url\": \"https://www.hivemq.com/edge/simulation/\",\n" +
            "            \"version\": \"1.0.0\",\n" +
            "            \"logoUrl\": \"icon-300x300.png\",\n" +
            "            \"author\": \"HiveMQ\",\n" +
            "            \"configSchema\": {\n" +
            "                \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "                \"type\": \"object\",\n" +
            "                \"properties\": {\n" +
            "                    \"host\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"Host name\",\n" +
            "                        \"description\": \"Host to connect to\",\n" +
            "                        \"format\": \"hostname\"\n" +
            "                    },\n" +
            "                    \"id\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"Identifier\",\n" +
            "                        \"description\": \"Unique identifier for this protocol adapter\",\n" +
            "                        \"minLength\": 1,\n" +
            "                        \"maxLength\": 1024,\n" +
            "                        \"format\": \"identifier\",\n" +
            "                        \"pattern\": \"([a-zA-Z_0-9\\\\-])*\"\n" +
            "                    },\n" +
            "                    \"pollingIntervalMillis\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"title\": \"Polling interval [ms]\",\n" +
            "                        \"description\": \"Interval in milliseconds to poll for changes\",\n" +
            "                        \"default\": 10000,\n" +
            "                        \"minimum\": 100.0,\n" +
            "                        \"maximum\": 8.64E+7\n" +
            "                    },\n" +
            "                    \"port\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"title\": \"Port\",\n" +
            "                        \"description\": \"Port to connect to\",\n" +
            "                        \"minimum\": 1.0,\n" +
            "                        \"maximum\": 65535.0\n" +
            "                    },\n" +
            "                    \"subscriptions\": {\n" +
            "                        \"title\": \"Subscriptions\",\n" +
            "                        \"description\": \"List of subscriptions for the simulation\",\n" +
            "                        \"type\": \"array\",\n" +
            "                        \"items\": {\n" +
            "                            \"type\": \"object\",\n" +
            "                            \"properties\": {\n" +
            "                                \"destination\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"title\": \"Destination Topic\",\n" +
            "                                    \"description\": \"The topic to publish data on\",\n" +
            "                                    \"format\": \"mqtt-topic\"\n" +
            "                                },\n" +
            "                                \"filter\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"title\": \"Data Topic\",\n" +
            "                                    \"description\": \"The local simulation filter topic\",\n" +
            "                                    \"format\": \"mqtt-topic\"\n" +
            "                                },\n" +
            "                                \"qos\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"QoS\",\n" +
            "                                    \"description\": \"MQTT quality of service level\",\n" +
            "                                    \"default\": 0,\n" +
            "                                    \"minimum\": 0.0,\n" +
            "                                    \"maximum\": 2.0\n" +
            "                                }\n" +
            "                            },\n" +
            "                            \"required\": [\n" +
            "                                \"destination\",\n" +
            "                                \"filter\",\n" +
            "                                \"qos\"\n" +
            "                            ],\n" +
            "                            \"title\": \"Subscriptions\",\n" +
            "                            \"description\": \"List of subscriptions for the simulation\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                \"required\": [\n" +
            "                    \"host\",\n" +
            "                    \"id\",\n" +
            "                    \"port\",\n" +
            "                    \"subscriptions\"\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"opc-ua-client\",\n" +
            "            \"protocol\": \"OPC-UA Client\",\n" +
            "            \"name\": \"OPC-UA to MQTT Protocol Adapter\",\n" +
            "            \"description\": \"Connects HiveMQ Edge to existing OPC-UA services as a client and enables a seamless exchange of data between MQTT and OPC-UA.\",\n" +
            "            \"url\": \"https://www.hivemq.com/edge/opc-ua/\",\n" +
            "            \"version\": \"1.0.0\",\n" +
            "            \"logoUrl\": \"opc.jpg\",\n" +
            "            \"author\": \"HiveMQ\",\n" +
            "            \"configSchema\": {\n" +
            "                \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "                \"type\": \"object\",\n" +
            "                \"properties\": {\n" +
            "                    \"auth\": {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"basic\": {\n" +
            "                                \"type\": \"object\",\n" +
            "                                \"properties\": {\n" +
            "                                    \"password\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Password\",\n" +
            "                                        \"description\": \"Password for basic authentication\"\n" +
            "                                    },\n" +
            "                                    \"username\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Username\",\n" +
            "                                        \"description\": \"Username for basic authentication\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"title\": \"Basic Authentication\",\n" +
            "                                \"description\": \"Username / password based authentication\"\n" +
            "                            },\n" +
            "                            \"x509\": {\n" +
            "                                \"type\": \"object\",\n" +
            "                                \"properties\": {\n" +
            "                                    \"enabled\": {\n" +
            "                                        \"type\": \"boolean\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"title\": \"X509 Authentication\",\n" +
            "                                \"description\": \"Authentication based on certificate / private key\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"id\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"Identifier\",\n" +
            "                        \"description\": \"Unique identifier for this protocol adapter\",\n" +
            "                        \"minLength\": 1,\n" +
            "                        \"maxLength\": 1024,\n" +
            "                        \"format\": \"identifier\",\n" +
            "                        \"pattern\": \"[a-z0-9_\\\\-].+\"\n" +
            "                    },\n" +
            "                    \"security\": {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"policy\": {\n" +
            "                                \"type\": \"string\",\n" +
            "                                \"enum\": [\n" +
            "                                    \"NONE\",\n" +
            "                                    \"BASIC128RSA15\",\n" +
            "                                    \"BASIC256\",\n" +
            "                                    \"BASIC256SHA256\",\n" +
            "                                    \"AES128_SHA256_RSAOAEP\",\n" +
            "                                    \"AES256_SHA256_RSAPSS\"\n" +
            "                                ],\n" +
            "                                \"title\": \"OPC UA security policy\",\n" +
            "                                \"description\": \"Security policy to use for communication with the server.\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"subscriptions\": {\n" +
            "                        \"type\": \"array\",\n" +
            "                        \"items\": {\n" +
            "                            \"type\": \"object\",\n" +
            "                            \"properties\": {\n" +
            "                                \"message-expiry-interval\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"MQTT message expiry interval [s]\",\n" +
            "                                    \"description\": \"Time in seconds until a MQTT message expires\",\n" +
            "                                    \"minimum\": 1.0,\n" +
            "                                    \"maximum\": 4294967295\n" +
            "                                },\n" +
            "                                \"mqtt-topic\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"title\": \"Destination MQTT topic\",\n" +
            "                                    \"description\": \"The MQTT topic to publish to\",\n" +
            "                                    \"format\": \"mqtt-topic\"\n" +
            "                                },\n" +
            "                                \"node\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"title\": \"Source Node ID\",\n" +
            "                                    \"description\": \"identifier of the node on the OPC-UA server. Example: \\\"ns=3;s=85/0:Temperature\\\"\"\n" +
            "                                },\n" +
            "                                \"payload-mode\": {\n" +
            "                                    \"type\": \"string\",\n" +
            "                                    \"enum\": [\n" +
            "                                        \"STRING\",\n" +
            "                                        \"JSON\"\n" +
            "                                    ],\n" +
            "                                    \"title\": \"Payload Mode\",\n" +
            "                                    \"description\": \"Format of the MQTT payload\"\n" +
            "                                },\n" +
            "                                \"publishing-interval\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"OPC UA publishing interval [ms]\",\n" +
            "                                    \"description\": \"OPC UA publishing interval in milliseconds for this subscription on the server\",\n" +
            "                                    \"default\": 1000,\n" +
            "                                    \"minimum\": 1.0\n" +
            "                                },\n" +
            "                                \"qos\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"MQTT QoS\",\n" +
            "                                    \"description\": \"MQTT quality of service level\",\n" +
            "                                    \"default\": 0,\n" +
            "                                    \"minimum\": 0.0,\n" +
            "                                    \"maximum\": 2.0\n" +
            "                                },\n" +
            "                                \"server-queue-size\": {\n" +
            "                                    \"type\": \"integer\",\n" +
            "                                    \"title\": \"OPC UA server queue size\",\n" +
            "                                    \"description\": \"OPC UA queue size for this subscription on the server\",\n" +
            "                                    \"default\": 1,\n" +
            "                                    \"minimum\": 1.0\n" +
            "                                }\n" +
            "                            },\n" +
            "                            \"required\": [\n" +
            "                                \"mqtt-topic\",\n" +
            "                                \"node\"\n" +
            "                            ]\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"tls\": {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"enabled\": {\n" +
            "                                \"type\": \"boolean\",\n" +
            "                                \"title\": \"Enable TLS\",\n" +
            "                                \"description\": \"Enables TLS encrypted connection\"\n" +
            "                            },\n" +
            "                            \"keystore\": {\n" +
            "                                \"type\": \"object\",\n" +
            "                                \"properties\": {\n" +
            "                                    \"password\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Keystore password\",\n" +
            "                                        \"description\": \"Password to open the keystore.\"\n" +
            "                                    },\n" +
            "                                    \"path\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Keystore path\",\n" +
            "                                        \"description\": \"Path on the local file system to the keystore.\"\n" +
            "                                    },\n" +
            "                                    \"private-key-password\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Private key password\",\n" +
            "                                        \"description\": \"Password to access the private key.\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"title\": \"Keystore\",\n" +
            "                                \"description\": \"Keystore that contains the client certificate including the chain. Required for X509 authentication.\"\n" +
            "                            },\n" +
            "                            \"truststore\": {\n" +
            "                                \"type\": \"object\",\n" +
            "                                \"properties\": {\n" +
            "                                    \"password\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Truststore password\",\n" +
            "                                        \"description\": \"Password to open the truststore.\"\n" +
            "                                    },\n" +
            "                                    \"path\": {\n" +
            "                                        \"type\": \"string\",\n" +
            "                                        \"title\": \"Truststore path\",\n" +
            "                                        \"description\": \"Path on the local file system to the truststore.\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"title\": \"Truststore\",\n" +
            "                                \"description\": \"Truststore wich contains the trusted server certificates or trusted intermediates.\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"uri\": {\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"title\": \"OPC-UA Server URI\",\n" +
            "                        \"description\": \"URI of the OPC-UA server to connect to\",\n" +
            "                        \"format\": \"uri\"\n" +
            "                    }\n" +
            "                },\n" +
            "                \"required\": [\n" +
            "                    \"id\",\n" +
            "                    \"uri\"\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";


    String EXAMPLE_DISCOVERY = "{\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"id\": \"holding-registers\",\n" +
            "      \"name\": \"Holding Registers\",\n" +
            "      \"description\": \"Holding Registers\",\n" +
            "      \"nodeType\": \"FOLDER\",\n" +
            "      \"selectable\": false,\n" +
            "      \"children\": [\n" +
            "        {\n" +
            "          \"id\": \"grouping-1\",\n" +
            "          \"name\": \"Addresses 1-16\",\n" +
            "          \"description\": \"\",\n" +
            "          \"nodeType\": \"FOLDER\",\n" +
            "          \"selectable\": false,\n" +
            "          \"children\": [\n" +
            "            {\n" +
            "              \"id\": \"address-location-1\",\n" +
            "              \"name\": \"1\",\n" +
            "              \"description\": \"\",\n" +
            "              \"nodeType\": \"VALUE\",\n" +
            "              \"selectable\": true,\n" +
            "              \"children\": [\n" +
            "                \n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    String EXAMPLE_ISA_95_JSON = "{\n" +
            "    \"enabled\": true,\n" +
            "    \"prefixAllTopics\": true,\n" +
            "    \"enterprise\": \"enterprise\",\n" +
            "    \"site\": \"site\",\n" +
            "    \"area\": \"area\",\n" +
            "    \"productionLine\": \"production-line\",\n" +
            "    \"workCell\": \"work-cell\"\n" +
            "}";

    //-- Connection Statis
    String EXAMPLE_STATUS_TRANSITION_RESULT = "{\n" +
            "    \"status\": \"PENDING\",\n" +
            "    \"callbackTimeoutMillis\": 1000" +
            "}";

    String EXAMPLE_EVENT_LIST = "{\n}";
}
