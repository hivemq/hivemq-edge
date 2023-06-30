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

public class CustomUserProperty {

    private final @NotNull String key;
    private final @NotNull String value;

    public static CustomUserProperty of(final @NotNull String key, final @NotNull String value){
        return new CustomUserProperty(key, value);
    }

    private CustomUserProperty(final @NotNull String key, final @NotNull String value) {
        this.key = key;
        this.value = value;
    }

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull String getValue() {
        return value;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserProperty)) return false;

        CustomUserProperty that = (CustomUserProperty) o;

        if (!key.equals(that.key)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "CustomUserProperty{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }
}
