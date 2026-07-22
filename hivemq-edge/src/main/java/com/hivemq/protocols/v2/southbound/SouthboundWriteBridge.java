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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.protocols.v2.northbound.ProtocolAdapterPublishIdentity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The producer side of the v2 southbound write path (EDG-824 #3): a v1-shaped
 * {@link WritingProtocolAdapter} the reused writing service drives when an MQTT publish arrives on a
 * southbound-mapped topic. It carries no protocol behavior of its own — it resolves the mapping's tag to the v2
 * {@link Node} and {@code tell}s a {@link ProtocolAdapterWrapperWriteRequest} to the wrapper mailbox, where the
 * write aspect batches it into the adapter's {@code writeBatch} on the actor's dispatch thread.
 * <p>
 * The {@link WritingOutput} is completed at enqueue: the mailbox accepted the write. The device-level outcome is
 * asynchronous and per-node — it is surfaced through the tag's write aspect (state, failure count, reason) in the
 * published snapshot.
 */
public final class SouthboundWriteBridge implements WritingProtocolAdapter {

    private final @NotNull ProtocolAdapterPublishIdentity identity;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull DataPointFactory dataPointFactory;

    private volatile @NotNull Map<String, Node> nodesByTagName;

    /**
     * @param identity         the v1-shaped identity of the owning v2 adapter.
     * @param wrapperSender    the owning wrapper's mailbox sender.
     * @param dataPointFactory the reused v1 factory the write value is carried with.
     * @param nodesByTagName   the adapter's nodes keyed by tag name.
     */
    public SouthboundWriteBridge(
            final @NotNull ProtocolAdapterPublishIdentity identity,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull Map<String, Node> nodesByTagName) {
        this.identity = identity;
        this.wrapperSender = wrapperSender;
        this.dataPointFactory = dataPointFactory;
        this.nodesByTagName = Map.copyOf(nodesByTagName);
    }

    /**
     * Replace the node set after a tags-only reload.
     *
     * @param nodesByTagName the adapter's current nodes keyed by tag name.
     */
    public void updateNodes(final @NotNull Map<String, Node> nodesByTagName) {
        this.nodesByTagName = Map.copyOf(nodesByTagName);
    }

    @Override
    public void write(final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {
        final String tagName = writingInput.getWritingContext().getTagName();
        final Node node = nodesByTagName.get(tagName);
        if (node == null) {
            writingOutput.fail("no tag [" + tagName + "] on v2 adapter [" + identity.getId() + "]");
            return;
        }
        final V2WritePayload payload = (V2WritePayload) writingInput.getWritingPayload();
        //noinspection ConstantValue — Jackson passes null for an absent "value" property despite the annotation
        if (payload.getValue() == null) {
            writingOutput.fail("southbound write payload for tag [" + tagName + "] has no \"value\" property");
            return;
        }
        final DataPoint value = dataPointFactory.createJsonDataPoint(tagName, payload.getValue());
        wrapperSender.tell(new ProtocolAdapterWrapperWriteRequest(node, value));
        writingOutput.finish();
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return V2WritePayload.class;
    }

    @Override
    public @NotNull String getId() {
        return identity.getId();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return identity.getProtocolAdapterInformation();
    }
}
