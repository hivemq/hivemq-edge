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
package com.hivemq.combining.model;

import com.hivemq.edge.api.model.DataCombiningSources;
import org.jetbrains.annotations.NotNull;

public enum PrimaryType {

    TAG,
    TOPIC_FILTER;

    public static @NotNull PrimaryType fromModel(@NotNull final com.hivemq.edge.api.model.DataCombiningSources.PrimaryTypeEnum type) {
        switch (type) {
            case TAG -> {
                return TAG;
            }
            case TOPIC_FILTER -> {
                return TOPIC_FILTER;
            }
        }
        throw new IllegalArgumentException();
    }

    public @NotNull com.hivemq.edge.api.model.DataCombiningSources.PrimaryTypeEnum toModel() {
        switch (this) {
            case TAG -> {
                return DataCombiningSources.PrimaryTypeEnum.TAG;
            }
            case TOPIC_FILTER -> {
                return DataCombiningSources.PrimaryTypeEnum.TOPIC_FILTER;
            }
        }
        throw new IllegalArgumentException();
    }


}
