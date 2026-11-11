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

/// Hands the node's declared TLS material to a surface that resolves references into it.
///
/// The adapter SDK's inputs carry no such thing and are not changed for it, so an adapter that
/// resolves shared material asks its input whether it is one of these. Edge's own input always is;
/// an input built anywhere else is not, and an adapter given one resolves against no declared
/// material.
public interface TLSMaterialSource {

    /// The node's provider, the one the configuration reader fills; never a fresh one.
    @NotNull
    TLSMaterialProvider tlsMaterialProvider();
}
