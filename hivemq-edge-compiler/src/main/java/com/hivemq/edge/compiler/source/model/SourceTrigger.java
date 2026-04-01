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
package com.hivemq.edge.compiler.source.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

/** Trigger for a combiner mapping. Exactly one of {@code tag} or {@code topicFilter} must be set. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceTrigger {

    /** Cross-adapter tag reference in the form {@code adapterId::tagName}. */
    public @Nullable String tag;

    public @Nullable String topicFilter;
}
