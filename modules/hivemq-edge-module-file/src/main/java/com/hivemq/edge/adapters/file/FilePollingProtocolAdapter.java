/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.file;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.file.config.FileAdapterConfig;
import com.hivemq.edge.adapters.file.config.FilePollingContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;


public class FilePollingProtocolAdapter implements PollingProtocolAdapter<FilePollingContext> {

    private final @NotNull FileAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<FilePollingContext> pollingContext;

    public FilePollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<FileAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.pollingContext = adapterConfig.getPollingContexts();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        // any setup which should be done before the adapter starts polling comes here.
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput, final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        protocolAdapterStopOutput.stoppedSuccessfully();
    }


    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull PollingInput<FilePollingContext> pollingInput, final @NotNull PollingOutput pollingOutput) {
        // absolute path to the file that contains the data. Magic string for now. Later it will be part of the config
        final String absolutePathToFle = pollingInput.getPollingContext().getFilePath();
        try {
            final Path path = Path.of(absolutePathToFle);
            final long length = path.toFile().length();
            final int limit = 64_000; // not a constant to have a more compact code example
            if (length > limit) {
                pollingOutput.fail(String.format("File '%s' of size '%d' exceeds the limit '%d'.", path.toAbsolutePath(), length, limit));
                return;
            }
            // load the content of the file
            byte[] fileContent = Files.readAllBytes(path);
            // encode it as base64
            final String encodedFileContent = Base64.getEncoder().encodeToString(fileContent);
            // add the content of the file to the output
            pollingOutput.addDataPoint("value", encodedFileContent);
        } catch (IOException e) {
            // in case something goes wrong while reading the file, an IOException will be thrown.
            // we handle it by failing the poll process and returning from the poll method.
            pollingOutput.fail(e, "An exception occurred while reading the file '" + absolutePathToFle + "'.");
            return;
        }
        // we need to tell edge that the polling is done as edge also supports asynchronous polling.
        pollingOutput.finish();
    }

    @Override
    public @NotNull List<FilePollingContext> getPollingContexts() {
        return pollingContext;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }
}
