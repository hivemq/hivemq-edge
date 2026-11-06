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

/// A password, either written in the configuration or named as a file to read it from.
///
/// Two shapes because a deployment has one or the other: an operator on Kubernetes projects a
/// secret into the pod and names the path, keeping the credential out of the configuration file,
/// the REST surface and version control; an operator on bare Docker or systemd has nowhere else to
/// put it and writes it inline.
///
/// `toString` is overridden on both so that neither a logged configuration nor a stack trace
/// reveals a password. The inline form holds the characters as written: a stage that hands them to
/// a key store cannot avoid having them in memory, so hiding them from the type would buy nothing.
public sealed interface Secret {

    /// The password as written in the configuration.
    record Literal(@NotNull String value) implements Secret {
        @Override
        public @NotNull String toString() {
            return "Secret.Literal(***)";
        }
    }

    /// The path of a file whose contents are the password. Not read until the loading stage.
    record FromFile(@NotNull String path) implements Secret {
        @Override
        public @NotNull String toString() {
            return "Secret.FromFile(" + path + ")";
        }
    }

    /// No password: the operator wrote that the store is to be read without checking its integrity.
    ///
    /// Distinct from an absent password, which a container store is opened with as an empty one and
    /// so still checked; only this waiver opens a store unchecked.
    record Waived() implements Secret {
        @Override
        public @NotNull String toString() {
            return "Secret.Waived";
        }
    }
}
