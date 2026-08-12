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
package com.hivemq.protocols.tag;

/**
 * The direction of the tag schema requested by a consumer. The values follow Edge's data-flow vocabulary
 * rather than read/write: "read/write" only holds for variable-shaped tags, while for method- or
 * condition-shaped tags the southbound schema is a request shape, not the writable subset of the northbound
 * one. The names are also the public REST vocabulary (the {@code direction} query parameter of
 * {@code GET /api/v1/management/protocol-adapters/schema/{adapterId}/{tagName}}).
 */
public enum TagSchemaDirection {
    /**
     * Northbound (adapter → broker, "read"): the full data shape published for the tag — {@code tagName},
     * {@code timestamp}, {@code value}, and any {@code metadata} / {@code context}.
     */
    NORTHBOUND,
    /**
     * Southbound (broker → adapter, "write"): the shape a write targets — only the {@code value}, with the
     * non-writable {@code tagName} / {@code timestamp} / {@code metadata} / {@code context} envelope dropped.
     */
    SOUTHBOUND
}
