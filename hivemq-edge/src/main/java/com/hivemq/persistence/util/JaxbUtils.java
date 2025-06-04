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

package com.hivemq.persistence.util;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.glassfish.jaxb.runtime.v2.JAXBContextFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public final class JaxbUtils {
    private static final @NotNull Map<String, ?> DEFAULT_PROPERTIES = new HashMap<>();
    private static final @NotNull JAXBContextFactory JAXB_CONTEXT_FACTORY = new JAXBContextFactory();

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T unmarshal(final @NotNull String xmlString, final @NotNull Class<T> clazz)
            throws JAXBException {
        final JAXBContext jaxbContext = JAXB_CONTEXT_FACTORY.createContext(new Class[]{clazz}, DEFAULT_PROPERTIES);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        try (final StringReader stringReader = new StringReader(xmlString)) {
            return (T) unmarshaller.unmarshal(stringReader);
        }
    }

    public static <T> void marshal(
            final @NotNull T object,
            final @NotNull File file,
            final @Nullable MarshallerConsumer marshallerConsumer) throws JAXBException {
        final JAXBContext jaxbContext =
                JAXB_CONTEXT_FACTORY.createContext(new Class[]{object.getClass()}, new HashMap<>());
        final Marshaller marshaller = jaxbContext.createMarshaller();
        if (marshallerConsumer != null) {
            marshallerConsumer.consume(marshaller);
        }
        marshaller.marshal(object, file);
    }

    public interface MarshallerConsumer {
        void consume(final @NotNull Marshaller marshaller) throws JAXBException;
    }
}
