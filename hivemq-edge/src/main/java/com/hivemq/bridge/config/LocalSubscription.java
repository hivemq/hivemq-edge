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
package com.hivemq.bridge.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocalSubscription {
    private final @NotNull List<String> filters;
    private final @Nullable String destination;
    private final @NotNull List<String> excludes;
    private final @NotNull List<CustomUserProperty> customUserProperties;
    private final boolean preserveRetain;
    private final int maxQoS;
    private @Nullable String uniqueId;

    public LocalSubscription(@NotNull final List<String> filters, @Nullable final String destination) {
        this.filters = filters;
        this.destination = destination;
        this.excludes = List.of();
        this.customUserProperties = List.of();
        this.maxQoS = 2;
        this.preserveRetain = false;
    }

    public LocalSubscription(
            @NotNull List<String> filters,
            @Nullable String destination,
            @NotNull List<String> excludes,
            @NotNull List<CustomUserProperty> customUserProperties,
            boolean preserveRetain,
            int maxQoS) {

        this.filters = filters;
        this.destination = destination;
        this.excludes = excludes;
        this.customUserProperties = customUserProperties;
        this.maxQoS = maxQoS;
        this.preserveRetain = preserveRetain;
    }

    public @NotNull List<String> getFilters() {
        return filters;
    }

    public @Nullable String getDestination() {
        return destination;
    }

    public @NotNull List<String> getExcludes() {
        return excludes;
    }

    public @NotNull List<CustomUserProperty> getCustomUserProperties() {
        return customUserProperties;
    }

    public boolean isPreserveRetain() {
        return preserveRetain;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalSubscription)) {
            return false;
        }

        LocalSubscription that = (LocalSubscription) o;

        if (preserveRetain != that.preserveRetain) {
            return false;
        }
        if (maxQoS != that.maxQoS) {
            return false;
        }
        if (!filters.equals(that.filters)) {
            return false;
        }
        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!excludes.equals(that.excludes)) {
            return false;
        }
        return customUserProperties.equals(that.customUserProperties);
    }

    @Override
    public int hashCode() {
        int result = filters.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + excludes.hashCode();
        result = 31 * result + customUserProperties.hashCode();
        result = 31 * result + (preserveRetain ? 1 : 0);
        result = 31 * result + maxQoS;
        return result;
    }

    public @NotNull String calculateUniqueId() {
        // hashcode wont work, it changes per start of the JVM. This means we would not find old queues of clients.
        // we generate the hash of all strings individually and then combine them in an operation that does not
        // take the order in account. otherwise changes in orders would lead to changes in the id, which potentially
        // leads to odd behavior if f.e. two filters get switched while parsing.

        if (uniqueId != null) {
            return uniqueId;
        }

        final MD5Digest md5Overall = new MD5Digest();
        final int digestSize = md5Overall.getDigestSize();
        byte[] digestOverAll = new byte[digestSize];

        if (!filters.isEmpty()) {
            final byte[] filtersAsBytes = XOR(filters);
            md5Overall.update(filtersAsBytes, 0, filtersAsBytes.length);
        }

        if (destination != null) {
            final byte[] destBytes = destination.getBytes(StandardCharsets.UTF_8);
            md5Overall.update(destBytes, 0, destBytes.length);
        }

        if (!excludes.isEmpty()) {
            final byte[] excludesAsBytes = XOR(excludes);
            md5Overall.update(excludesAsBytes, 0, excludesAsBytes.length);
        }


        if (!customUserProperties.isEmpty()) {
            final List<String> keyValueCombined = new ArrayList<>();
            for (CustomUserProperty customUserProperty : customUserProperties) {
                keyValueCombined.add(customUserProperty.getKey() + "/" + customUserProperty.getValue());
            }
            final byte[] keyValueCombinedAsBytes = XOR(keyValueCombined);
            md5Overall.update(keyValueCombinedAsBytes, 0, keyValueCombinedAsBytes.length);
        }

        byte[] preserveRetainBytes = ("" + preserveRetain).getBytes(StandardCharsets.UTF_8);
        md5Overall.update(preserveRetainBytes, 0, preserveRetainBytes.length);

        byte[] maxQoSBytes = ("" + maxQoS).getBytes(StandardCharsets.UTF_8);
        md5Overall.update(maxQoSBytes, 0, maxQoSBytes.length);

        md5Overall.doFinal(digestOverAll, 0);
        uniqueId = Base64.toBase64String(digestOverAll);
        return uniqueId;
    }


    private byte[] getMd5DigestIndependentOfOrder(final @NotNull List<String> input) {
        final MD5Digest md5Overall = new MD5Digest();
        final int digestSize = md5Overall.getDigestSize();
        byte[] digestOverAll = new byte[digestSize];

        for (String element : input) {
            byte[] digest = getMd5Digest(element);
            for (int i = 0; i < digestSize; i++) {
                digestOverAll[i] = (byte) (digestOverAll[i] ^ digest[i]);
            }
        }
        return digestOverAll;
    }


    private byte[] XOR(List<String> strings) {
        final ArrayList<byte[]> stringsAsBytes = new ArrayList<>();

        // we need to find the longest string
        // meanwhile we can already populate the byte array
        int maxValue = -1;
        for (String string : strings) {
            final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            stringsAsBytes.add(bytes);
            if (bytes.length > maxValue) {
                maxValue = bytes.length;
            }
        }

        byte[] result = new byte[maxValue];
        for (byte[] stringsAsByte : stringsAsBytes) {
            for (int i = 0; i < stringsAsByte.length; i++) {
                result[i] ^= stringsAsByte[i];
            }
        }
        return result;
    }

    private byte[] getMd5Digest(final @NotNull String input) {
        MD5Digest md5 = new MD5Digest();
        final byte[] filterBytes = input.getBytes(StandardCharsets.UTF_8);
        md5.update(filterBytes, 0, filterBytes.length);
        byte[] digest = new byte[md5.getDigestSize()];
        md5.doFinal(digest, 0);
        return digest;
    }

}
