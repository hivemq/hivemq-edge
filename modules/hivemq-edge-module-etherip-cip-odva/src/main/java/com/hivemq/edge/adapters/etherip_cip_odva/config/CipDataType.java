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

import java.util.OptionalInt;
import org.jetbrains.annotations.NotNull;

public enum CipDataType {
    BOOL,
    SINT, // 8 bit signed
    USINT, // 8 bit unsigned
    INT, // 16 bit signed
    UINT, // 16 bit unsigned
    DINT, // 32 but signed
    UDINT, // 32 bit unsigned
    LINT, // 64 bit signed
    REAL, // float (32 bit)
    LREAL, // double (64 bit)
    SSTRING, // USINT, 8 bit length
    STRING, // SINT, 16 bit length

    // BYTE // 8 bit string
    // WORD // 16 bit string
    // DWORD // 32 bit string
    // LWORD // 64 bit string

    COMPOSITE; // artificial type to indicate creation of a composite of all tags at given tag address

    /**
     * The fixed number of bytes one element of this type occupies on the wire, or empty if the width is not
     * statically known. Strings ({@link #SSTRING}/{@link #STRING}) are variable-length (a length prefix plus the
     * character bytes), and {@link #COMPOSITE} is not a wire type at all, so those return empty. The widths mirror
     * the encoders (see {@code CipTagEncoders}); this is the single source of truth for byte-layout validation.
     */
    public @NotNull OptionalInt staticByteWidth() {
        return switch (this) {
            case BOOL, SINT, USINT -> OptionalInt.of(1);
            case INT, UINT -> OptionalInt.of(2);
            case DINT, UDINT, REAL -> OptionalInt.of(4);
            case LINT, LREAL -> OptionalInt.of(8);
            case SSTRING, STRING, COMPOSITE -> OptionalInt.empty();
        };
    }
}
