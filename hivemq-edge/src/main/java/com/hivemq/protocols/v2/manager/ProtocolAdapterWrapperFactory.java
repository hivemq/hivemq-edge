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
package com.hivemq.protocols.v2.manager;

import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a fully-wired, dispatcher-attached {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapper} from one
 * read-only {@link ProtocolAdapterEntity} and its type's {@link ProtocolAdapterFactory} (design §8.2). The seam the
 * manager depends on so its supervision logic is testable without the full actor stack: production wiring uses
 * {@link DefaultProtocolAdapterWrapperFactory}; the manager tests inject a recording double.
 */
public interface ProtocolAdapterWrapperFactory {

    /**
     * Construct, wire, and attach a wrapper/adapter pair for the given configuration. The returned adapter is
     * <b>not yet started</b> — the manager brings it to its config-declared goal with an explicit activation
     * command (design §9.3). Implementations must not perform I/O or connect.
     *
     * @param entity         the adapter configuration to instantiate.
     * @param factory        the factory of the configuration's protocol-adapter type.
     * @param healthListener the seam the new wrapper tells its {@code started} / {@code stopped} / {@code error}
     *                       transitions through.
     * @return the managed adapter (handle plus teardown resources), ready to be registered and started.
     * @throws ProtocolAdapterConfigException if the configuration cannot be turned into a wrapper (for
     *                                               example a {@code node-string} that does not deserialize into
     *                                               the type's node class).
     */
    @NotNull
    ProtocolAdapterContainer create(
            @NotNull ProtocolAdapterEntity entity,
            @NotNull ProtocolAdapterFactory factory,
            @NotNull ProtocolAdapterWrapperEventListener healthListener);

    /**
     * Translate the configuration's tags into the runtime node/tag pairs — the payload of an in-place
     * {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.UpdateTagSet} (design §8.2). Shares the
     * exact node-string deserialization {@link #create} uses, so a tags-only reload builds the same pairs the
     * adapter was created with.
     *
     * @param entity  the adapter configuration whose tags to translate.
     * @param factory the factory of the configuration's protocol-adapter type.
     * @return the node/tag pairs in declaration order.
     * @throws ProtocolAdapterConfigException if a {@code node-string} does not deserialize into the type's
     *                                               node class.
     */
    @NotNull
    List<NodeTagPair> translateNodes(@NotNull ProtocolAdapterEntity entity, @NotNull ProtocolAdapterFactory factory);
}
