/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.etherip_cip_odva.config;

/**
 * Declares whether a write covers the whole CIP attribute or only part of it. Only meaningful when the tag's
 * {@link CipReadWrite} allows writing.
 * <p>
 * A CIP attribute is the atomic write unit: one {@code Set_Attribute_Single} always replaces the entire
 * attribute — a device rejects a request that does not carry the attribute's full byte width. Since a single
 * attribute often packs several tags, this mode declares whether the tag(s) supplied by a write span the whole
 * attribute (so it can be written directly) or only part of it (so the rest must be preserved).
 */
public enum CipWriteMode {
    /**
     * The supplied tag(s) cover the entire attribute, so the write carries the whole attribute directly, with
     * no device read. The user guarantees the configured tag(s) at this address span the full attribute width;
     * if they do not, the device rejects the (too-short) request.
     */
    COMPLETE_WRITE,
    /**
     * The supplied tag(s) cover only part of the attribute, so the rest must be preserved. Implemented as a
     * read-modify-write: read the current attribute from the device, overlay the supplied tag value(s), write
     * the whole attribute back.
     * <p>
     * The read is a raw device read ({@code Get_Attribute_Single}), independent of the tag's
     * {@link CipReadWrite} direction — {@code WRITE_ONLY} governs only whether the adapter polls/publishes the
     * tag northbound, not whether the device attribute can be read. So a {@code WRITE_ONLY} tag may use
     * {@code PARTIAL_WRITE}; it just needs the device to permit reading that attribute (which the read-modify-
     * write will attempt, and fail with an error if the device refuses).
     */
    PARTIAL_WRITE
}
