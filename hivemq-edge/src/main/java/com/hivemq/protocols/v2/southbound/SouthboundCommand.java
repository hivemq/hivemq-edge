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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * One southbound command leased from a {@link SouthboundWriteBacklog}: the value to write plus the opaque
 * {@code id} the delivery side hands back to {@link SouthboundWriteBacklog#delete} once the command reaches a
 * terminal outcome — committed after the device acknowledged it, or dead-lettered after the device refused it.
 * Backed in production by an MQTT client-queue message (the id is its unique message id).
 *
 * @param id    the store's opaque handle for this command (stable until it is deleted).
 * @param value the reused v1 value to write.
 */
public record SouthboundCommand(@NotNull String id, @NotNull DataPoint value) {}
