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

import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Where one piece of TLS material comes from, named but not opened.
///
/// A truststore, an allow-list and a revocation list each resolve to a set, so an operator may name
/// several files, whole directories, glob patterns and the runtime's own truststore together, and
/// the contents are merged. An identity resolves to one key, so its file set names a single file -
/// which the loading stage enforces rather than the type.
///
/// Expansion happens in the loading stage. Nothing here is checked for existence, so two file sets
/// naming the same things are equal whatever is on disk.
public record FileSet(
        /// Individual files. Each must exist: a file that is absent is an error, since there is no
        /// set that could be empty.
        @NotNull List<String> files,
        /// Directories, every readable entry of which belongs to this material.
        @NotNull List<Directory> directories,
        /// Glob patterns, every match of which belongs to this material.
        @NotNull List<Pattern> patterns,
        /// Truststores the runtime carries rather than the operator supplies, by name.
        ///
        /// Named beside the three above rather than instead of them, so a surface trusting the
        /// public authorities and a plant authority together names both and gets the union.
        @NotNull List<String> builtins) {

    /// The public certificate authorities a JVM ships with, at `$JAVA_HOME/lib/security/cacerts` by
    /// default. The only name [#builtins()] takes; the loading stage refuses any other.
    public static final @NotNull String JVM_CACERTS = "JVM-cacerts";

    /// A directory whose readable entries belong to this material, and whether it may hold none.
    ///
    /// Two fields rather than a bare path, because emptiness is a question only the operator can
    /// answer: a directory a deployment populates later is legitimately empty today, while an empty
    /// directory where material was expected means Edge checks less than the configuration says.
    ///
    /// `mayBeEmpty` defaults to false, so the strict reading is what an operator gets without
    /// writing anything, and permitting emptiness is a visible choice.
    public record Directory(@NotNull String location, boolean mayBeEmpty) {

        /// A directory that must hold at least one file.
        public Directory(final @NotNull String location) {
            this(location, false);
        }
    }

    /// A glob pattern whose matches belong to this material, and whether it may match none. Same two
    /// fields and same default as [Directory], for the same reason.
    public record Pattern(@NotNull String location, boolean mayBeEmpty) {

        /// A pattern that must match at least one file.
        public Pattern(final @NotNull String location) {
            this(location, false);
        }
    }

    public FileSet {
        files = List.copyOf(files);
        directories = List.copyOf(directories);
        patterns = List.copyOf(patterns);
        builtins = List.copyOf(builtins);
    }

    /// One named file, which is how every surface writes its material today.
    public static @NotNull FileSet of(final @NotNull String file) {
        return new FileSet(List.of(file), List.of(), List.of(), List.of());
    }

    /// The public certificate authorities the runtime carries, and nothing else.
    public static @NotNull FileSet ofJvmCacerts() {
        return new FileSet(List.of(), List.of(), List.of(), List.of(JVM_CACERTS));
    }

    /// Whether this names nothing at all, which the loading stage refuses.
    public boolean isEmpty() {
        return files.isEmpty() && directories.isEmpty() && patterns.isEmpty() && builtins.isEmpty();
    }

    /// Whether this names a path an operator gave, which is what an integrity check applies to.
    ///
    /// A file set naming only a runtime truststore needs no password: Edge read nothing the operator
    /// supplied, so there is nothing to claim is unchanged.
    public boolean namesAPath() {
        return !files.isEmpty() || !directories.isEmpty() || !patterns.isEmpty();
    }
}
