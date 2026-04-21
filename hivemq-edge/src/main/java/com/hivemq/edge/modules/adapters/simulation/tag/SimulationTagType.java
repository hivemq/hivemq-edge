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
package com.hivemq.edge.modules.adapters.simulation.tag;

public enum SimulationTagType {

    /**
     * Default behavior when no {@code randomValue} or {@code staticValue} block is configured on a tag.
     * Emits a random double bounded by the adapter-level {@code minValue}/{@code maxValue}.
     */
    LEGACY_RANDOM_DOUBLE,

    /**
     * Per-tag bounded random INT / LONG / DOUBLE (see {@link SimulationValueType}).
     */
    RANDOM_NUMBER,

    /**
     * Per-tag constant INT / LONG / DOUBLE / STRING (see {@link SimulationValueType}).
     */
    STATIC_VALUE
}
