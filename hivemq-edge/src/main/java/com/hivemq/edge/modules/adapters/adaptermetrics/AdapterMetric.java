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
package com.hivemq.edge.modules.adapters.adaptermetrics;

import org.jetbrains.annotations.NotNull;

public enum AdapterMetric {
    READ_PUBLISH_SUCCESS("read.publish.success.count"),
    READ_PUBLISH_FAILED("read.publish.failed.count"),
    WRITE_PUBLISH_SUCCESS("write.publish.success.count"),
    WRITE_PUBLISH_FAILED("write.publish.failed.count"),
    CONNECTION_SUCCESS("connection.success.count"),
    CONNECTION_FAILED("connection.failed.count");

    private final @NotNull String suffix;

    AdapterMetric(final @NotNull String suffix) {
        this.suffix = suffix;
    }

    public @NotNull String getSuffix() {
        return suffix;
    }
}
