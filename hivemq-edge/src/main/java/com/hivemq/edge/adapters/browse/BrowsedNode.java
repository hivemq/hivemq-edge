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
package com.hivemq.edge.adapters.browse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single node discovered during a bulk browse of a device address space.
 * Contains informational fields from the device plus generated default values for tag/topic names.
 *
 * @param nodePath              Slash-separated path from root. E.g. {@code /Data/Static/Int32}.
 * @param namespaceUri          Protocol-specific context identifier. For OPC UA this is the namespace URI
 *                              (e.g. {@code urn:eclipse:milo:hello-world}); for other protocols it is
 *                              typically the adapter ID.
 * @param namespaceIndex        Protocol-specific context index (informational only). For OPC UA this is the
 *                              namespace index; for other protocols it may be a device instance number,
 *                              unit ID, or simply 0.
 * @param nodeId                Full device-specific identifier. E.g. {@code ns=2;s=HelloWorld/ScalarTypes/Int32}
 *                              for OPC UA, a symbolic tag name for EtherNet/IP, or a composite key for Modbus.
 * @param dataType              Device data type name. E.g. {@code Int32}, {@code String}, {@code Boolean}.
 * @param accessLevel           Device access level. E.g. {@code READ}, {@code READ_WRITE}, {@code WRITE}.
 * @param nodeDescription       Description from the device. May be null.
 * @param tagNameDefault        Suggested tag name generated from adapterId and browse name.
 * @param tagDescription        Suggested tag description. Defaults to nodeDescription.
 * @param northboundTopicDefault Suggested northbound MQTT topic.
 * @param southboundTopicDefault Suggested southbound MQTT topic filter.
 */
public record BrowsedNode(
        @NotNull String nodePath,
        @NotNull String namespaceUri,
        int namespaceIndex,
        @NotNull String nodeId,
        @NotNull String dataType,
        @NotNull String accessLevel,
        @Nullable String nodeDescription,
        @NotNull String tagNameDefault,
        @Nullable String tagDescription,
        @NotNull String northboundTopicDefault,
        @NotNull String southboundTopicDefault) {}
