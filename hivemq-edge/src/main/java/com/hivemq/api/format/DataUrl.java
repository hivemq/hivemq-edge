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
package com.hivemq.api.format;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@JsonSerialize(using = DataUrl.Serializer.class)
public class DataUrl {

    public static final String BASE64_TOKEN = ";base64";
    public static final String DATA_TOKEN = "data:";


    private final @NotNull String mimeType;
    private final @NotNull String encoding;
    private final @NotNull String charset;
    private final @NotNull String data;

    DataUrl(
            final @NotNull String mimeType,
            final @NotNull String charSet,
            final @NotNull String encoding,
            final @NotNull String data) {
        this.mimeType = mimeType;
        this.charset = charSet;
        this.encoding = encoding;
        this.data = data;
    }

    public static @NotNull DataUrl createBase64JsonDataUrl(final @NotNull String data) {
        return new DataUrl("application/json", StandardCharsets.US_ASCII.displayName(), "base64", data);
    }


    public static @NotNull DataUrl create(final @NotNull String dataUrlAsString) {
        checkDataPrefix(dataUrlAsString);
        // remove data:
        final String dataUrlWithoutDataPrefix = dataUrlAsString.substring(5);
        // split meta data and data  on the ','
        checkSeparator(dataUrlWithoutDataPrefix, dataUrlAsString);
        final String[] metaDataAndDataSplit = dataUrlWithoutDataPrefix.split(",");
        if (metaDataAndDataSplit.length != 2) {
            throw new IllegalArgumentException(dataUrlAsString +
                    " is not a valid data URL, because it did not contain metadata and data separated by exactly one ','.");
        }
        final String metadata = metaDataAndDataSplit[0];
        final String data = metaDataAndDataSplit[1];
        if (!metadata.contains(BASE64_TOKEN)) {
            throw new IllegalArgumentException("Only base64 encoding is allowed for data URLs.");
        }
        final String metaDataWithoutEncoding = metadata.replaceAll(BASE64_TOKEN, "");
        if (metaDataWithoutEncoding.contains(";charset=")) {
            final String[] mimeAndCharSetSplit = metaDataWithoutEncoding.split(";charset=");
            final String mimeType = mimeAndCharSetSplit[0];
            final String charset = mimeAndCharSetSplit[1];
            return new DataUrl(mimeType, charset, "base64", data);
        } else {
            return new DataUrl(metaDataWithoutEncoding, StandardCharsets.US_ASCII.displayName(), "base64", data);
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataUrl dataUrl = (DataUrl) o;
        return mimeType.equals(dataUrl.mimeType) &&
                encoding.equals(dataUrl.encoding) &&
                charset.equals(dataUrl.charset) &&
                data.equals(dataUrl.data);
    }

    @Override
    public int hashCode() {
        int result = mimeType.hashCode();
        result = 31 * result + encoding.hashCode();
        result = 31 * result + charset.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }

    private static void checkDataPrefix(final @NotNull String dataUrlAsString) {
        if (!dataUrlAsString.startsWith(DATA_TOKEN)) {
            throw new IllegalArgumentException("The supplied String '" +
                    dataUrlAsString +
                    "' does not start with the required prefix '" +
                    DATA_TOKEN +
                    "'.");
        }
    }

    private static void checkSeparator(
            final @NotNull String dataUrlWithoutDataPrefix, final @NotNull String dataUrlAsString) {
        if (!dataUrlWithoutDataPrefix.contains(",")) {
            throw new IllegalArgumentException("The supplied String '" +
                    dataUrlAsString +
                    "' does not contain the necessary ',' as separator between metadata and data.");
        }
    }


    public @NotNull String getCharset() {
        return charset;
    }

    public @NotNull String getData() {
        return data;
    }

    public @NotNull String getEncoding() {
        return encoding;
    }

    public @NotNull String getMimeType() {
        return mimeType;
    }

    @Override
    public @NotNull String toString() {
        return "data:" + mimeType + ";" + encoding + "," + data;
    }


    public static class Serializer extends StdSerializer<DataUrl> {

        public Serializer() {
            this(null);
        }

        public Serializer(final @NotNull Class<DataUrl> t) {
            super(t);
        }

        @Override
        public void serialize(final DataUrl value, final JsonGenerator gen, final SerializerProvider provider)
                throws IOException {
            gen.writeString(value.toString());
        }
    }
}
