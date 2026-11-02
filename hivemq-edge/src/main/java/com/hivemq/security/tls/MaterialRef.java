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
package com.hivemq.security.tls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Where one piece of TLS material is to be found, without having looked.
///
/// A [TLSConfig] names its material through these: nothing here has been opened, so a surface
/// adapter can produce a complete configuration without touching a filesystem. Resolving a name or
/// uuid into a location, and a location into bytes, happen in later stages.
///
/// The three shapes are mutually exclusive, which the factory methods enforce. A reference by name
/// or uuid points into a declared `tls-material` element; `inline` is the material written where it
/// is used, which is what every adapter and bridge configuration does today.
public sealed interface MaterialRef {

    /// A reference naming a `tls-material` and an entry within it, written `material::entry`.
    record ByName(@NotNull String material, @NotNull String entry) implements MaterialRef {}

    /// A reference by the uuid an entry carries, unique across every `tls-material` and type.
    record ByUuid(@NotNull String uuid) implements MaterialRef {}

    /// Material written where it is used, naming files directly.
    ///
    /// @param format the container or encoding, or `null` where the type admits only one
    /// @param files the files, directories and glob patterns naming this material's contents
    /// @param password the container password, or `null`
    /// @param entry the key entry to select from a container holding several, or `null`
    /// @param entryPassword the password for that one key entry, or `null`
    record Inline(
            @Nullable String format,
            @NotNull FileSet files,
            @Nullable Secret password,
            @Nullable String entry,
            @Nullable Secret entryPassword)
            implements MaterialRef {}
}
