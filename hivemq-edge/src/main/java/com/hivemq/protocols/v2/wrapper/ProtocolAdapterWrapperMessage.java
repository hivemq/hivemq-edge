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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;

/**
 * The single mailbox message type of the {@link ProtocolAdapterWrapper} actor (design §6.1). Everything the
 * wrapper consumes arrives as one of these four:
 * <ul>
 * <li>{@link ProtocolAdapterWrapperCommand} — goal and lifecycle commands ({@code CONTROL} band) handled via the
 * goal-command bypass, valid in every state;</li>
 * <li>{@link ProtocolAdapterWrapperEvent} — protocol-adapter events and timer expiries, the only messages that
 * flow through the transition table;</li>
 * <li>{@link ProtocolAdapterWrapperTick} — time, delivered as a message ({@code TICK} band);</li>
 * <li>{@link ProtocolAdapterWrapperWriteRequest} — a southbound write to route to a tag's write aspect
 * ({@code DATA} band, design §7.5), neither a goal command nor a transition-table event.</li>
 * </ul>
 * Sealed because all permitted subtypes live in this package (a sealed interface and its {@code permits} must
 * share a package); the generic {@link MailboxMessage} marker is the non-sealed bridge that lets each extend it
 * across packages.
 */
public sealed interface ProtocolAdapterWrapperMessage extends MailboxMessage
        permits ProtocolAdapterWrapperCommand,
                ProtocolAdapterWrapperEvent,
                ProtocolAdapterWrapperTick,
                ProtocolAdapterWrapperWriteRequest {}
