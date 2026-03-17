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
package com.hivemq.protocols.fsm;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.protocols.ProtocolAdapterStartOutputImpl;
import com.hivemq.protocols.ProtocolAdapterStopOutputImpl;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge that wraps an existing {@link ProtocolAdapter} (old SDK interface) and exposes it
 * as a {@link ProtocolAdapter2} (new FSM interface).
 * <p>
 * This allows the FSM-based wrapper and manager to work with existing adapter implementations
 * without requiring them to be rewritten.
 * <p>
 * Mapping:
 * <ul>
 *   <li>{@link #connect}(NORTHBOUND) calls {@link ProtocolAdapter#start} and blocks until the output signals completion</li>
 *   <li>{@link #connect}(SOUTHBOUND) is a no-op (old adapters handle southbound internally during start)</li>
 *   <li>{@link #disconnect}(NORTHBOUND) calls {@link ProtocolAdapter#stop} and blocks until the output signals completion</li>
 *   <li>{@link #disconnect}(SOUTHBOUND) is a no-op</li>
 *   <li>{@link #precheck} is a no-op (old adapters don't have this concept)</li>
 *   <li>{@link #supportsSouthbound} checks for {@link ProtocolAdapterCapability#WRITE}</li>
 * </ul>
 */
public class ProtocolAdapter2Bridge implements ProtocolAdapter2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapter2Bridge.class);

    private final @NotNull ProtocolAdapter delegate;
    private final @NotNull ModuleServices moduleServices;

    public ProtocolAdapter2Bridge(
            final @NotNull ProtocolAdapter delegate, final @NotNull ModuleServices moduleServices) {
        this.delegate = delegate;
        this.moduleServices = moduleServices;
    }

    @Override
    public @NotNull String getId() {
        return delegate.getId();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return delegate.getProtocolAdapterInformation();
    }

    @Override
    public boolean supportsSouthbound() {
        return delegate.getProtocolAdapterInformation().getCapabilities().contains(ProtocolAdapterCapability.WRITE);
    }

    @Override
    public void precheck() throws ProtocolAdapterException {
        // Old adapters don't have a precheck concept - no-op
    }

    @Override
    public void connect(final @NotNull ConnectionContext context) throws ProtocolAdapterException {
        if (context.getDirection() == ConnectionContext.Direction.SOUTHBOUND) {
            // Old adapters handle southbound internally during start - no separate action needed
            return;
        }
        final ProtocolAdapterStartInput input = () -> moduleServices;
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        delegate.start(input, output);
        try {
            output.getStartFuture().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProtocolAdapterException("Adapter start interrupted for '" + getId() + "'", e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ProtocolAdapterException(
                    "Adapter start failed for '" + getId() + "': " + output.getMessage(), cause);
        }
    }

    @Override
    public void disconnect(final @NotNull ConnectionContext context) {
        if (context.getDirection() == ConnectionContext.Direction.SOUTHBOUND) {
            // Old adapters handle southbound internally during stop - no separate action needed
            return;
        }
        final ProtocolAdapterStopInput input = new ProtocolAdapterStopInput() {};
        final ProtocolAdapterStopOutputImpl output = new ProtocolAdapterStopOutputImpl();
        delegate.stop(input, output);
        try {
            output.getOutputFuture().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Adapter stop interrupted for '{}'", getId(), e);
        } catch (final ExecutionException e) {
            LOGGER.warn("Adapter stop failed for '{}': {}", getId(), output.getErrorMessage(), e);
        }
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }
}
