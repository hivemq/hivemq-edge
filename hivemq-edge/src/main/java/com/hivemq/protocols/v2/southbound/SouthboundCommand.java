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
 * One southbound command read from a {@link SouthboundCommandSource}: the value to write plus the opaque {@code id}
 * the source uses to {@link SouthboundCommandSource#commit commit}, {@link SouthboundCommandSource#release release},
 * or {@link SouthboundCommandSource#deadLetter dead-letter} it once its outcome is known. Backed in production by an
 * MQTT client-queue message (the id is its unique message id).
 *
 * @param id    the source's opaque handle for this command (stable until committed/dead-lettered).
 * @param value the reused v1 value to write.
 */
public record SouthboundCommand(@NotNull String id, @NotNull DataPoint value) {}
