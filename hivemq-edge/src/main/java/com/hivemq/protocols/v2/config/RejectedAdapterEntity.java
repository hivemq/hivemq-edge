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

import org.jetbrains.annotations.NotNull;

/**
 * A v2 adapter entry whose configuration failed validation and that has no previously-applied
 * configuration to fall back to. The extractor scopes the failure to this one adapter — the rest of the section is
 * applied normally — and the manager surfaces it as an {@code ERROR} adapter, visible via REST, with no impact on the
 * node or on any sibling adapter.
 *
 * @param entity the invalid configuration as loaded (its {@code adapter-id} is guaranteed non-empty).
 * @param reason the joined human-readable validation failures.
 */
public record RejectedAdapterEntity(
        @NotNull ProtocolAdapterEntity entity, @NotNull String reason) {}
