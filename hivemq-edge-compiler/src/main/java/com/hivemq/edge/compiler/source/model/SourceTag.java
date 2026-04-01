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

/**
 * A named handle for a device tag. References a DEVICE-TAG by {@code deviceTagId} or includes it inline via
 * {@code deviceTag}. Exactly one must be present; both null means a standalone named tag with no device point.
 *
 * <p>Inline form:
 *
 * <pre>{@code
 * name: NozzlePressure
 * deviceTag:
 *   id: "ns=2;i=1003"
 *   dataType: Float
 * }</pre>
 *
 * <p>By-id reference form:
 *
 * <pre>{@code
 * name: ExtruderSpeed
 * deviceTagId: "ns=2;s=Extruder.Zone1.Speed"
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceTag {

    public @Nullable String name;

    /** Inline DEVICE-TAG — mutually exclusive with {@code deviceTagId}. */
    public @Nullable SourceDeviceTag deviceTag;

    /** Reference to a DEVICE-TAG defined elsewhere in the same adapter directory. */
    public @Nullable String deviceTagId;
}
