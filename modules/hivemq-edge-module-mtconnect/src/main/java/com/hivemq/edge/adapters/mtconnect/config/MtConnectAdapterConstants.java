/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.config;

public interface MtConnectAdapterConstants {
    boolean DEFAULT_ALLOW_UNTRUSTED_CERTIFICATES = false;
    int DEFAULT_POLLING_INTERVAL_MILLIS = 1000;
    int DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS = 5;
    int MIN_HTTP_CONNECT_TIMEOUT_SECONDS = 1;
    int MAX_HTTP_CONNECT_TIMEOUT_SECONDS = 60;
    int DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL = 10;
}
