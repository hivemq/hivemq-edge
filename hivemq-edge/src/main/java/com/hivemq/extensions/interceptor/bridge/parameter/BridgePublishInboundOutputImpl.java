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
package com.hivemq.extensions.interceptor.bridge.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.bridge.parameter.BridgePublishInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;

import java.util.concurrent.atomic.AtomicBoolean;

public class BridgePublishInboundOutputImpl extends AbstractAsyncOutput<BridgePublishInboundOutput>
        implements BridgePublishInboundOutput {

    private final @NotNull ModifiablePublishPacketImpl publishPacket;
    private final @NotNull AtomicBoolean preventDelivery = new AtomicBoolean(false);

    public BridgePublishInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiablePublishPacketImpl publishPacket) {
        super(asyncer);
        this.publishPacket = publishPacket;
    }

    @Override
    public @NotNull ModifiablePublishPacketImpl getPublishPacket() {
        return publishPacket;
    }

    @Override
    public void preventPublishDelivery() {
        checkPrevented();
    }

    public void forciblyPreventPublishDelivery() {
        this.preventDelivery.set(true);
    }

    public boolean isPreventDelivery() {
        return preventDelivery.get();
    }

    private void checkPrevented() {
        if (!preventDelivery.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("preventPublishDelivery must not be called more than once");
        }
    }

    public @NotNull BridgePublishInboundOutputImpl update(final @NotNull BridgePublishInboundInputImpl input) {
        return new BridgePublishInboundOutputImpl(asyncer, publishPacket.update(input.getPublishPacket()));
    }


}
