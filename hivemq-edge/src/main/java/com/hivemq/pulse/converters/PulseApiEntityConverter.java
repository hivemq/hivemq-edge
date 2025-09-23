/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.converters;

import org.jetbrains.annotations.NotNull;

/**
 * A generic interface for converting between REST entities and internal entities.
 * This interface defines methods for converting data models used in REST APIs
 * to their corresponding internal representations and vice versa.
 *
 * @param <RestEntity>     the type representing the REST entity.
 * @param <InternalEntity> the type representing the internal entity.
 */
public interface PulseApiEntityConverter<RestEntity, InternalEntity> {

    /**
     * Converts a REST entity to an internal entity.
     *
     * @param restEntity the REST entity to convert. Must not be null.
     * @return the converted internal entity. Will not be null.
     */
    @NotNull InternalEntity toInternalEntity(final @NotNull RestEntity restEntity);

    /**
     * Converts an internal entity to a REST entity.
     *
     * @param internalEntity the internal entity to convert. Must not be null.
     * @return the converted REST entity. Will not be null.
     */
    @NotNull RestEntity toRestEntity(final @NotNull InternalEntity internalEntity);
}
