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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Assets;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Devices;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Error;
import static com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchemaType.Streams;

public enum MtConnectSchema {
    Assets_1_2(Assets, 1, 2, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_2.MTConnectAssetsType.class),
    Assets_1_3(Assets, 1, 3, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_3.MTConnectAssetsType.class),
    Assets_1_4(Assets, 1, 4, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_4.MTConnectAssetsType.class),
    Assets_1_5(Assets, 1, 5, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_5.MTConnectAssetsType.class),
    Assets_1_6(Assets, 1, 6, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_6.MTConnectAssetsType.class),
    Assets_1_7(Assets, 1, 7, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7.MTConnectAssetsType.class),
    Assets_1_8(Assets, 1, 8, com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8.MTConnectAssetsType.class),
    Assets_2_0(Assets, 2, 0, null),
    Assets_2_1(Assets, 2, 1, null),
    Assets_2_2(Assets, 2, 2, null),
    Assets_2_3(Assets, 2, 3, null),
    Assets_2_4(Assets, 2, 4, null),
    Devices_1_0(Devices, 1, 0, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_0.MTConnectDevicesType.class),
    Devices_1_1(Devices, 1, 1, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_1.MTConnectDevicesType.class),
    Devices_1_2(Devices, 1, 2, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_2.MTConnectDevicesType.class),
    Devices_1_3(Devices, 1, 3, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3.MTConnectDevicesType.class),
    Devices_1_4(Devices, 1, 4, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4.MTConnectDevicesType.class),
    Devices_1_5(Devices, 1, 5, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_5.MTConnectDevicesType.class),
    Devices_1_6(Devices, 1, 6, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_6.MTConnectDevicesType.class),
    Devices_1_7(Devices, 1, 7, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_7.MTConnectDevicesType.class),
    Devices_1_8(Devices, 1, 8, com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_8.MTConnectDevicesType.class),
    Devices_2_0(Devices, 2, 0, null),
    Devices_2_1(Devices, 2, 1, null),
    Devices_2_2(Devices, 2, 2, null),
    Devices_2_3(Devices, 2, 3, null),
    Devices_2_4(Devices, 2, 4, null),
    Error_1_1(Error, 1, 1, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_1.MTConnectErrorType.class),
    Error_1_2(Error, 1, 2, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_2.MTConnectErrorType.class),
    Error_1_3(Error, 1, 3, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_3.MTConnectErrorType.class),
    Error_1_4(Error, 1, 4, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_4.MTConnectErrorType.class),
    Error_1_5(Error, 1, 5, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_5.MTConnectErrorType.class),
    Error_1_6(Error, 1, 6, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_6.MTConnectErrorType.class),
    Error_1_7(Error, 1, 7, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_7.MTConnectErrorType.class),
    Error_1_8(Error, 1, 8, com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_8.MTConnectErrorType.class),
    Error_2_0(Error, 2, 0, null),
    Error_2_1(Error, 2, 1, null),
    Error_2_2(Error, 2, 2, null),
    Error_2_3(Error, 2, 3, null),
    Error_2_4(Error, 2, 4, null),
    Streams_1_1(Streams, 1, 1, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_1.MTConnectStreamsType.class),
    Streams_1_2(Streams, 1, 2, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_2.MTConnectStreamsType.class),
    Streams_1_3(Streams, 1, 3, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3.MTConnectStreamsType.class),
    Streams_1_4(Streams, 1, 4, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_4.MTConnectStreamsType.class),
    Streams_1_5(Streams, 1, 5, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_5.MTConnectStreamsType.class),
    Streams_1_6(Streams, 1, 6, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_6.MTConnectStreamsType.class),
    Streams_1_7(Streams, 1, 7, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_7.MTConnectStreamsType.class),
    Streams_1_8(Streams, 1, 8, com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_8.MTConnectStreamsType.class),
    Streams_2_0(Streams, 2, 0, null),
    Streams_2_1(Streams, 2, 1, null),
    Streams_2_2(Streams, 2, 2, null),
    Streams_2_3(Streams, 2, 3, null),
    Streams_2_4(Streams, 2, 4, null),
    ;

    private static final @NotNull Map<String, MtConnectSchema> LOCATION_TO_SCHEMA_MAP =
            Stream.of(values()).collect(Collectors.toMap(MtConnectSchema::getLocation, Function.identity()));
    private static final @NotNull String SCHEMA_LOCATION = "schemaLocation";
    private static final @NotNull String XMLSCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final @NotNull String MT_CONNECT = "MTConnect";
    private static final @NotNull Pattern PATTERN_LOCATION =
            Pattern.compile("MTConnect(Assets|Devices|Error|Streams)_(\\d+)\\.(\\d+)\\.xsd$");
    private final int majorVersion;
    private final int minorVersion;
    private final @NotNull String location;
    private final @NotNull MtConnectSchemaType type;
    private final @Nullable Class<?> mtConnectType;
    private @Nullable JAXBContext jaxbContext;

    MtConnectSchema(
            @NotNull final MtConnectSchemaType type,
            final int majorVersion,
            final int minorVersion,
            final @Nullable Class<?> mtConnectType) {
        this.location = "urn:mtconnect.org:MTConnect" +
                type.name() +
                ":" +
                majorVersion +
                "." +
                minorVersion +
                " /schemas/MTConnect" +
                type.name() +
                "_" +
                majorVersion +
                "." +
                minorVersion +
                ".xsd";
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.type = type;
        this.mtConnectType = mtConnectType;
    }

    public static @Nullable MtConnectSchema of(final @Nullable String location) {
        MtConnectSchema mtConnectSchema = LOCATION_TO_SCHEMA_MAP.get(location);
        if (mtConnectSchema == null && location != null) {
            final Matcher matcher = PATTERN_LOCATION.matcher(location);
            if (matcher.find()) {
                final MtConnectSchemaType type = MtConnectSchemaType.of(matcher.group(1));
                final int majorVersion = Integer.parseInt(matcher.group(2));
                final int minorVersion = Integer.parseInt(matcher.group(3));
                switch (type) {
                    case Assets -> {
                        switch (majorVersion) {
                            case 1 -> {
                                switch (minorVersion) {
                                    case 2 -> mtConnectSchema = Assets_1_2;
                                    case 3 -> mtConnectSchema = Assets_1_3;
                                    case 4 -> mtConnectSchema = Assets_1_4;
                                    case 5 -> mtConnectSchema = Assets_1_5;
                                    case 6 -> mtConnectSchema = Assets_1_6;
                                    case 7 -> mtConnectSchema = Assets_1_7;
                                    case 8 -> mtConnectSchema = Assets_1_8;
                                }
                            }
                            case 2 -> {
                                switch (minorVersion) {
                                    case 0 -> mtConnectSchema = Assets_2_0;
                                    case 1 -> mtConnectSchema = Assets_2_1;
                                    case 2 -> mtConnectSchema = Assets_2_2;
                                    case 3 -> mtConnectSchema = Assets_2_3;
                                    case 4 -> mtConnectSchema = Assets_2_4;
                                }
                            }
                        }
                    }
                    case Devices -> {
                        switch (majorVersion) {
                            case 1 -> {
                                switch (minorVersion) {
                                    case 0 -> mtConnectSchema = Devices_1_0;
                                    case 1 -> mtConnectSchema = Devices_1_1;
                                    case 2 -> mtConnectSchema = Devices_1_2;
                                    case 3 -> mtConnectSchema = Devices_1_3;
                                    case 4 -> mtConnectSchema = Devices_1_4;
                                    case 5 -> mtConnectSchema = Devices_1_5;
                                    case 6 -> mtConnectSchema = Devices_1_6;
                                    case 7 -> mtConnectSchema = Devices_1_7;
                                    case 8 -> mtConnectSchema = Devices_1_8;
                                }
                            }
                            case 2 -> {
                                switch (minorVersion) {
                                    case 0 -> mtConnectSchema = Devices_2_0;
                                    case 1 -> mtConnectSchema = Devices_2_1;
                                    case 2 -> mtConnectSchema = Devices_2_2;
                                    case 3 -> mtConnectSchema = Devices_2_3;
                                    case 4 -> mtConnectSchema = Devices_2_4;
                                }
                            }
                        }
                    }
                    case Error -> {
                        switch (majorVersion) {
                            case 1 -> {
                                switch (minorVersion) {
                                    case 1 -> mtConnectSchema = Error_1_1;
                                    case 2 -> mtConnectSchema = Error_1_2;
                                    case 3 -> mtConnectSchema = Error_1_3;
                                    case 4 -> mtConnectSchema = Error_1_4;
                                    case 5 -> mtConnectSchema = Error_1_5;
                                    case 6 -> mtConnectSchema = Error_1_6;
                                    case 7 -> mtConnectSchema = Error_1_7;
                                    case 8 -> mtConnectSchema = Error_1_8;
                                }
                            }
                            case 2 -> {
                                switch (minorVersion) {
                                    case 0 -> mtConnectSchema = Error_2_0;
                                    case 1 -> mtConnectSchema = Error_2_1;
                                    case 2 -> mtConnectSchema = Error_2_2;
                                    case 3 -> mtConnectSchema = Error_2_3;
                                    case 4 -> mtConnectSchema = Error_2_4;
                                }
                            }
                        }
                    }
                    case Streams -> {
                        switch (majorVersion) {
                            case 1 -> {
                                switch (minorVersion) {
                                    case 1 -> mtConnectSchema = Streams_1_1;
                                    case 2 -> mtConnectSchema = Streams_1_2;
                                    case 3 -> mtConnectSchema = Streams_1_3;
                                    case 4 -> mtConnectSchema = Streams_1_4;
                                    case 5 -> mtConnectSchema = Streams_1_5;
                                    case 6 -> mtConnectSchema = Streams_1_6;
                                    case 7 -> mtConnectSchema = Streams_1_7;
                                    case 8 -> mtConnectSchema = Streams_1_8;
                                }
                            }
                            case 2 -> {
                                switch (minorVersion) {
                                    case 0 -> mtConnectSchema = Streams_2_0;
                                    case 1 -> mtConnectSchema = Streams_2_1;
                                    case 2 -> mtConnectSchema = Streams_2_2;
                                    case 3 -> mtConnectSchema = Streams_2_3;
                                    case 4 -> mtConnectSchema = Streams_2_4;
                                }
                            }
                        }
                    }
                }
            }
        }
        return mtConnectSchema;
    }

    public static @Nullable String extractSchemaLocation(final @NotNull String str) {
        try (final StringReader stringReader = new StringReader(str)) {
            return extractSchemaLocation(stringReader);
        }
    }

    public static @Nullable String extractSchemaLocation(final @NotNull Reader reader) {
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
            while (xmlStreamReader.hasNext()) {
                if (xmlStreamReader.next() == XMLStreamConstants.START_ELEMENT &&
                        xmlStreamReader.getLocalName().startsWith(MT_CONNECT)) {
                    for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                        final String attributeName = xmlStreamReader.getAttributeLocalName(i);
                        final String attributeNamespace = xmlStreamReader.getAttributeNamespace(i);
                        if (SCHEMA_LOCATION.equals(attributeName) && XMLSCHEMA_INSTANCE.equals(attributeNamespace)) {
                            return xmlStreamReader.getAttributeValue(i);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (xmlStreamReader != null) {
                try {
                    xmlStreamReader.close();
                } catch (XMLStreamException ignored) {
                }
            }
        }
        return null;
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

    public @Nullable Unmarshaller getUnmarshaller() throws JAXBException {
        if (mtConnectType == null) {
            return null;
        }
        if (jaxbContext == null) {
            // There is no additional synchronization.
            // So there might be contention, but that's a one-time cost.
            // The overall performance is better than a synchronized block.
            jaxbContext = JAXBContext.newInstance(mtConnectType);
        }
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setEventHandler(new MtConnectSchemaValidationEventHandler());
        return unmarshaller;
    }
}
