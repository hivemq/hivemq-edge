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

import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.jetbrains.annotations.NotNull;

/**
 * The broker collaborators the southbound write path stands on, bundled so the wrapper factory carries one
 * optional dependency instead of four: the topic tree the intake subscribes on, the durable client queue the
 * backlogs lease from, the publish service the verdicts go out through, and the retained-message store the
 * crash-replay dedup recovers the last verdict from. Absent as a whole in unit rigs — the plane then falls back
 * to the interim in-memory backlogs.
 *
 * @param topicTree                  the broker topic tree.
 * @param clientQueuePersistence     the durable client queue store.
 * @param internalPublishService     the broker-internal publish path (verdicts).
 * @param retainedMessagePersistence the retained-message store (verdict recovery).
 */
public record SouthboundBrokerRuntime(
        @NotNull LocalTopicTree topicTree,
        @NotNull ClientQueuePersistence clientQueuePersistence,
        @NotNull InternalPublishService internalPublishService,
        @NotNull RetainedMessagePersistence retainedMessagePersistence) {}
