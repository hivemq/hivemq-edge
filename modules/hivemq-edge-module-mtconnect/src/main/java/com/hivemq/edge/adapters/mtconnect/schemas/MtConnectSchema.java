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
package com.hivemq.edge.adapters.mtconnect.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Assets;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Devices;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Error;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Streams;

public enum MtConnectSchema {
    Assets_1_2(Assets, 1, 2, "urn:mtconnect.org:MTConnectAssets:1.2 /schemas/MTConnectAssets_1.2.xsd"),
    Assets_1_3(Assets, 1, 3, "urn:mtconnect.org:MTConnectAssets:1.3 /schemas/MTConnectAssets_1.3.xsd"),
    Assets_1_4(Assets, 1, 4, "urn:mtconnect.org:MTConnectAssets:1.4 /schemas/MTConnectAssets_1.4.xsd"),
    Assets_1_5(Assets, 1, 5, "urn:mtconnect.org:MTConnectAssets:1.5 /schemas/MTConnectAssets_1.5.xsd"),
    Assets_1_6(Assets, 1, 6, "urn:mtconnect.org:MTConnectAssets:1.6 /schemas/MTConnectAssets_1.6.xsd"),
    Assets_1_7(Assets, 1, 7, "urn:mtconnect.org:MTConnectAssets:1.7 /schemas/MTConnectAssets_1.7.xsd"),
    Assets_1_8(Assets, 1, 8, "urn:mtconnect.org:MTConnectAssets:1.8 /schemas/MTConnectAssets_1.8.xsd"),
    Assets_2_0(Assets, 2, 0, "urn:mtconnect.org:MTConnectAssets:2.0 /schemas/MTConnectAssets_2.0.xsd"),
    Assets_2_1(Assets, 2, 1, "urn:mtconnect.org:MTConnectAssets:2.1 /schemas/MTConnectAssets_2.1.xsd"),
    Assets_2_2(Assets, 2, 2, "urn:mtconnect.org:MTConnectAssets:2.2 /schemas/MTConnectAssets_2.2.xsd"),
    Assets_2_3(Assets, 2, 3, "urn:mtconnect.org:MTConnectAssets:2.3 /schemas/MTConnectAssets_2.3.xsd"),
    Devices_1_1(Devices, 1, 1, "urn:mtconnect.org:MTConnectDevices:1.1 /schemas/MTConnectDevices_1.1.xsd"),
    Devices_1_2(Devices, 1, 2, "urn:mtconnect.org:MTConnectDevices:1.2 /schemas/MTConnectDevices_1.2.xsd"),
    Devices_1_3(Devices, 1, 3, "urn:mtconnect.org:MTConnectDevices:1.3 /schemas/MTConnectDevices_1.3.xsd"),
    Devices_1_4(Devices, 1, 4, "urn:mtconnect.org:MTConnectDevices:1.4 /schemas/MTConnectDevices_1.4.xsd"),
    Devices_1_5(Devices, 1, 5, "urn:mtconnect.org:MTConnectDevices:1.5 /schemas/MTConnectDevices_1.5.xsd"),
    Devices_1_6(Devices, 1, 6, "urn:mtconnect.org:MTConnectDevices:1.6 /schemas/MTConnectDevices_1.6.xsd"),
    Devices_1_7(Devices, 1, 7, "urn:mtconnect.org:MTConnectDevices:1.7 /schemas/MTConnectDevices_1.7.xsd"),
    Devices_1_8(Devices, 1, 8, "urn:mtconnect.org:MTConnectDevices:1.8 /schemas/MTConnectDevices_1.8.xsd"),
    Devices_2_0(Devices, 2, 0, "urn:mtconnect.org:MTConnectDevices:2.0 /schemas/MTConnectDevices_2.0.xsd"),
    Devices_2_1(Devices, 2, 1, "urn:mtconnect.org:MTConnectDevices:2.1 /schemas/MTConnectDevices_2.1.xsd"),
    Devices_2_2(Devices, 2, 2, "urn:mtconnect.org:MTConnectDevices:2.2 /schemas/MTConnectDevices_2.2.xsd"),
    Devices_2_3(Devices, 2, 3, "urn:mtconnect.org:MTConnectDevices:2.3 /schemas/MTConnectDevices_2.3.xsd"),
    Error_1_1(Error, 1, 1, "urn:mtconnect.org:MTConnectError:1.1 /schemas/MTConnectError_1.1.xsd"),
    Error_1_2(Error, 1, 2, "urn:mtconnect.org:MTConnectError:1.2 /schemas/MTConnectError_1.2.xsd"),
    Error_1_3(Error, 1, 3, "urn:mtconnect.org:MTConnectError:1.3 /schemas/MTConnectError_1.3.xsd"),
    Error_1_4(Error, 1, 4, "urn:mtconnect.org:MTConnectError:1.4 /schemas/MTConnectError_1.4.xsd"),
    Error_1_5(Error, 1, 5, "urn:mtconnect.org:MTConnectError:1.5 /schemas/MTConnectError_1.5.xsd"),
    Error_1_6(Error, 1, 6, "urn:mtconnect.org:MTConnectError:1.6 /schemas/MTConnectError_1.6.xsd"),
    Error_1_7(Error, 1, 7, "urn:mtconnect.org:MTConnectError:1.7 /schemas/MTConnectError_1.7.xsd"),
    Error_1_8(Error, 1, 8, "urn:mtconnect.org:MTConnectError:1.8 /schemas/MTConnectError_1.8.xsd"),
    Error_2_0(Error, 2, 0, "urn:mtconnect.org:MTConnectError:2.0 /schemas/MTConnectError_2.0.xsd"),
    Error_2_1(Error, 2, 1, "urn:mtconnect.org:MTConnectError:2.1 /schemas/MTConnectError_2.1.xsd"),
    Error_2_2(Error, 2, 2, "urn:mtconnect.org:MTConnectError:2.2 /schemas/MTConnectError_2.2.xsd"),
    Error_2_3(Error, 2, 3, "urn:mtconnect.org:MTConnectError:2.3 /schemas/MTConnectError_2.3.xsd"),
    Streams_1_1(Streams, 1, 1, "urn:mtconnect.org:MTConnectStreams:1.1 /schemas/MTConnectStreams_1.1.xsd"),
    Streams_1_2(Streams, 1, 2, "urn:mtconnect.org:MTConnectStreams:1.2 /schemas/MTConnectStreams_1.2.xsd"),
    Streams_1_3(Streams, 1, 3, "urn:mtconnect.org:MTConnectStreams:1.3 /schemas/MTConnectStreams_1.3.xsd"),
    Streams_1_4(Streams, 1, 4, "urn:mtconnect.org:MTConnectStreams:1.4 /schemas/MTConnectStreams_1.4.xsd"),
    Streams_1_5(Streams, 1, 5, "urn:mtconnect.org:MTConnectStreams:1.5 /schemas/MTConnectStreams_1.5.xsd"),
    Streams_1_6(Streams, 1, 6, "urn:mtconnect.org:MTConnectStreams:1.6 /schemas/MTConnectStreams_1.6.xsd"),
    Streams_1_7(Streams, 1, 7, "urn:mtconnect.org:MTConnectStreams:1.7 /schemas/MTConnectStreams_1.7.xsd"),
    Streams_1_8(Streams, 1, 8, "urn:mtconnect.org:MTConnectStreams:1.8 /schemas/MTConnectStreams_1.8.xsd"),
    Streams_2_0(Streams, 2, 0, "urn:mtconnect.org:MTConnectStreams:2.0 /schemas/MTConnectStreams_2.0.xsd"),
    Streams_2_1(Streams, 2, 1, "urn:mtconnect.org:MTConnectStreams:2.1 /schemas/MTConnectStreams_2.1.xsd"),
    Streams_2_2(Streams, 2, 2, "urn:mtconnect.org:MTConnectStreams:2.2 /schemas/MTConnectStreams_2.2.xsd"),
    Streams_2_3(Streams, 2, 3, "urn:mtconnect.org:MTConnectStreams:2.3 /schemas/MTConnectStreams_2.3.xsd"),
    ;

    private static final @NotNull Map<String, MtConnectSchema> LOCATION_TO_SCHEMA_MAP =
            Stream.of(values()).collect(Collectors.toMap(MtConnectSchema::getLocation, Function.identity()));
    private final int majorVersion;
    private final int minorVersion;
    private final @NotNull String location;
    private final @NotNull MtConnectSchemaType type;

    MtConnectSchema(
            @NotNull final MtConnectSchemaType type,
            final int majorVersion,
            final int minorVersion,
            @NotNull final String location) {
        this.location = location;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.type = type;
    }

    public static @Nullable MtConnectSchema of(final @NotNull String location) {
        return LOCATION_TO_SCHEMA_MAP.get(location);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public @NotNull String getLocation() {
        return location;
    }

    public @NotNull MtConnectSchemaType getType() {
        return type;
    }
}
