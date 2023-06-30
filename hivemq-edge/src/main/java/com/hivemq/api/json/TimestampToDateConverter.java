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
package com.hivemq.api.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Strings;
import com.hivemq.api.error.ApiException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimestampToDateConverter {

    private static final DateTimeFormatter DATE_FORMAT_OUTBOUND =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);

    public static class Serializer extends JsonSerializer<Long> {

        @Override
        public void serialize(
                final @Nullable Long value,
                final @NotNull JsonGenerator jsonGenerator,
                final @NotNull SerializerProvider provider) throws IOException {
            if (value == null) {
                jsonGenerator.writeNull();
            } else {
                final ZonedDateTime zonedDateTime =
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
                jsonGenerator.writeString(zonedDateTime.format(DATE_FORMAT_OUTBOUND));
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<Long> {

        @Override
        public @Nullable Long deserialize(
                final @NotNull JsonParser jsonParser,
                final @NotNull DeserializationContext ctxt) throws IOException {
            final String dateAsString = jsonParser.getText();
            if (Strings.isNullOrEmpty(dateAsString)) {
                return null;
            } else {
                try {
                    final ZonedDateTime parse = ZonedDateTime.parse(dateAsString, DateTimeFormatter.ISO_DATE_TIME);
                    return parse.toInstant().toEpochMilli();
                } catch (final Throwable t) {
                    throw new ApiException("Illegal value '" +
                            dateAsString +
                            "' for date");
                }
            }
        }
    }

}
