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
package com.hivemq.protocols.v2.config;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The extractor's consumer payload for one applied {@code <v2>} section: the adapter configurations
 * accepted (including any previously-applied configuration retained because its replacement was invalid) and the new
 * adapter entries rejected by validation. Configuration errors are always scoped to the single adapter they belong
 * to — a bad adapter never rejects the section, and never takes the node down.
 *
 * @param adapters the complete set of accepted adapter configurations, in declaration order.
 * @param rejected the new adapter entries whose configuration failed validation, surfaced as {@code ERROR} adapters.
 */
public record ProtocolAdapterConfigUpdate(
        @NotNull List<ProtocolAdapterEntity> adapters,
        @NotNull List<RejectedAdapterEntity> rejected) {}
