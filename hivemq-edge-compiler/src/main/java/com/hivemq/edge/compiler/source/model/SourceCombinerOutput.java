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

@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceCombinerOutput {

    public @Nullable String topic;

    /**
     * QoS for the combiner output topic. Parsed but not used by the runtime — the Edge combiner model has no
     * per-output QoS. Declared as {@code @Nullable Integer} so the compiler can detect when the user explicitly
     * specifies it (non-null → emit {@code COMBINER_OUTPUT_QOS_IGNORED} warning); {@code null} means not specified.
     */
    public @Nullable Integer qos;
}
