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
 * Mechanism used when writing a tag. Only meaningful when the tag's {@link CipReadWrite} allows writing.
 * <p>
 * A CIP attribute is the atomic write unit: one {@code Set_Attribute_Single} writes the whole attribute.
 * Since a single attribute often packs several tags, the mode decides what happens to the bytes not
 * supplied by this write.
 */
public enum CipWriteMode {
    /**
     * Write the supplied tag value(s) and zero the rest of the attribute. No device read.
     * Correct for a complete composite or a write-only attribute; destructive on a partial scalar write.
     */
    OVERWRITE_ZERO,
    /**
     * Read the current attribute, overlay the supplied tag value(s), write the whole attribute back.
     * Preserves the bytes of sibling tags not supplied. Requires the attribute to be readable.
     */
    READ_MODIFY_WRITE
}
