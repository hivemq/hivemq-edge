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
package com.hivemq.edge.compiler.source.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Publishes a TAG's value to an MQTT topic.
 *
 * <p>By-name reference form:
 *
 * <pre>{@code
 * tagName: NozzlePressure
 * topic: factory/berlin/extruder-01/nozzle-pressure
 * qos: 1
 * }</pre>
 *
 * <p>Inline TAG form:
 *
 * <pre>{@code
 * topic: factory/berlin/extruder-01/nozzle-pressure
 * qos: 1
 * tag:
 *   name: NozzlePressure
 *   deviceTag:
 *     id: "ns=2;i=1003"
 *     dataType: Float
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceNorthboundMapping {

    /** Reference to a TAG by name — mutually exclusive with {@code tag}. */
    public @Nullable String tagName;

    /** Inline TAG — mutually exclusive with {@code tagName}. */
    public @Nullable SourceTag tag;

    public @Nullable String topic;
    public int qos = 1;

    // Optional overrides — compiler applies documented defaults when absent
    public @Nullable Boolean includeTagNames; // default: false
    public @Nullable Boolean includeTimestamp; // default: true
    public @Nullable Boolean includeMetadata; // default: false
    public @Nullable Long messageExpiryInterval; // default: Long.MAX_VALUE

    /** Source position — set by {@code YamlFileParser} after parsing; -1 means unknown. 0-based (LSP convention). */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int line = -1;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int character = -1;
}
