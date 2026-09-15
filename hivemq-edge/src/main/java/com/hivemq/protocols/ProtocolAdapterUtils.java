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
package com.hivemq.protocols;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolAdapterUtils {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterUtils.class);

    public static @NotNull ObjectMapper createProtocolAdapterMapper(final @NotNull ObjectMapper objectMapper) {
        final ObjectMapper adapterMapper = objectMapper.copy();
        adapterMapper
                .coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        adapterMapper
                .coercionConfigFor(LogicalType.Collection)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        adapterMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        adapterMapper.addHandler(new ReportUnknownPropertyHandler());
        final SimpleModule module = new SimpleModule("UserPropertyModule", Version.unknownVersion());
        module.setAbstractTypes(new SimpleAbstractTypeResolver());
        adapterMapper.registerModule(module);
        return adapterMapper;
    }

    /**
     * A copy of the mapper that reports settings an adapter configuration does not know, instead of
     * discarding them without a word.
     *
     * <p>Separate from {@link #createProtocolAdapterMapper} on purpose: that one is used to read adapter
     * configurations arriving over the REST API and applies coercions this path does not want. Reading
     * the configuration <em>file</em> goes through {@link ProtocolAdapterConfigConverter}, which holds
     * the application-wide mapper directly — so a handler added only to the other one never runs on the
     * path an operator's file actually takes.
     */
    public static @NotNull ObjectMapper withUnknownPropertyReporting(final @NotNull ObjectMapper objectMapper) {
        final ObjectMapper reportingMapper = objectMapper.copy();
        reportingMapper.addHandler(new ReportUnknownPropertyHandler());
        return reportingMapper;
    }

    /**
     * Reports a setting the adapter configuration does not know, then carries on without it.
     *
     * <p>The application-wide mapper disables {@code FAIL_ON_UNKNOWN_PROPERTIES}, so a misspelled
     * element in an adapter configuration used to be discarded in complete silence: the operator wrote
     * a setting, the adapter started without it, and nothing anywhere said so. A misspelled enum
     * <em>value</em> already fails that adapter's conversion with an error naming the value; this
     * handler closes the gap for a misspelled setting <em>name</em>, which Jackson would otherwise
     * drop without a word.
     *
     * <p>Reporting rather than throwing is deliberate. Configurations written for other versions of
     * HiveMQ Edge may legitimately carry settings this one does not know, and refusing to start an
     * adapter over one is a worse outcome than running it without the setting; the operator still
     * learns about it. Omitting a setting is not the same as writing it wrongly, but it is not always
     * harmless either: the certificate-validation axes default to their strictest value, so a
     * discarded axis yields more checking — but the preset door defaults to {@code STANDARD}, which is
     * weaker than {@code ALL}, so a discarded preset can yield less checking than the operator wrote.
     *
     * <p>A problem handler is consulted before {@code FAIL_ON_UNKNOWN_PROPERTIES} is examined, so this
     * makes the behaviour identical whichever way the base mapper is configured — which also stops
     * tests that build their own mapper from pinning semantics the product does not have.
     */
    private static final class ReportUnknownPropertyHandler extends DeserializationProblemHandler {

        @Override
        public boolean handleUnknownProperty(
                final @NotNull DeserializationContext context,
                final @NotNull JsonParser parser,
                final @NotNull JsonDeserializer<?> deserializer,
                final @NotNull Object beanOrClass,
                final @NotNull String propertyName)
                throws IOException {

            final List<String> known = knownPropertiesOf(deserializer);
            if (known.isEmpty()) {
                LOGGER.warn(
                        "Adapter configuration: '{}' is not a known setting of '{}' and has been ignored. The adapter "
                                + "is running without it; correct or remove the entry.",
                        propertyName,
                        simpleNameOf(beanOrClass));
            } else {
                LOGGER.warn(
                        "Adapter configuration: '{}' is not a known setting of '{}' and has been ignored. The adapter "
                                + "is running without it; correct or remove the entry. Known settings: {}.",
                        propertyName,
                        simpleNameOf(beanOrClass),
                        String.join(", ", known));
            }
            parser.skipChildren();
            return true;
        }

        private static @NotNull String simpleNameOf(final @NotNull Object beanOrClass) {
            return beanOrClass instanceof final Class<?> type
                    ? type.getSimpleName()
                    : beanOrClass.getClass().getSimpleName();
        }

        private static @NotNull List<String> knownPropertiesOf(final @Nullable JsonDeserializer<?> deserializer) {
            if (!(deserializer instanceof final BeanDeserializerBase bean)) {
                return List.of();
            }
            final List<String> names = new ArrayList<>();
            for (final Iterator<SettableBeanProperty> it = bean.properties(); it.hasNext(); ) {
                names.add(it.next().getName());
            }
            return names;
        }
    }
}
