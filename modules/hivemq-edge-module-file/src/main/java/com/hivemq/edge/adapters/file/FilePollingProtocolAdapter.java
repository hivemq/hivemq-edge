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
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.file.config.FileAdapterConfig;
import com.hivemq.edge.adapters.file.config.FileToMqttMapping;
import com.hivemq.edge.adapters.file.convertion.MappingException;
import com.hivemq.edge.adapters.file.tag.FileTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class FilePollingProtocolAdapter implements PollingProtocolAdapter<FileToMqttMapping> {

    private static final @NotNull org.slf4j.Logger LOG = LoggerFactory.getLogger(FilePollingProtocolAdapter.class);

    private final @NotNull FileAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<FileToMqttMapping> pollingContext;

    public FilePollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<FileAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.pollingContext = adapterConfig.getFileToMqttConfig().getMappings();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        // any setup which should be done before the adapter starts polling comes here.
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        protocolAdapterStopOutput.stoppedSuccessfully();
    }


    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull PollingInput<FileToMqttMapping> pollingInput, final @NotNull PollingOutput pollingOutput) {
        adapterConfig.getTags().stream()
                .filter(tag -> tag.getName().equals(pollingInput.getPollingContext().getTagName()))
                .findFirst()
                .ifPresentOrElse(
                        def -> pollFile(pollingInput, pollingOutput, def),
                        () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                );
    }

    private static void pollFile(
            @NotNull PollingInput<FileToMqttMapping> pollingInput,
            @NotNull PollingOutput pollingOutput,
            Tag<FileTagDefinition> fileTag) {
        final String absolutePathToFle = fileTag.getDefinition().getFilePath();
        try {
            final Path path = Path.of(absolutePathToFle);
            final long length = path.toFile().length();
            final int limit = 64_000; // not a constant to have a more compact code example
            if (length > limit) {
                pollingOutput.fail(String.format("File '%s' of size '%d' exceeds the limit '%d'.",
                        path.toAbsolutePath(),
                        length,
                        limit));
                return;
            }
            final byte[] fileContent = Files.readAllBytes(path);
            final Object value = pollingInput.getPollingContext().getContentType().map(fileContent);
            pollingOutput.addDataPoint("value", value);
            pollingOutput.finish();
        } catch (IOException e) {
            LOG.warn("An exception occurred while reading the file '{}'.", absolutePathToFle, e);
            pollingOutput.fail(e, "An exception occurred while reading the file '" + absolutePathToFle + "'.");
        } catch (MappingException e){
            LOG.warn("An exception occurred while converting the data in file '{}' to a payload '{}'.", absolutePathToFle, e.getMessage());
            pollingOutput.fail(e, "An exception occurred while reading the file '" + absolutePathToFle + "'.");
        }
    }

    @Override
    public @NotNull List<FileToMqttMapping> getPollingContexts() {
        return pollingContext;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getFileToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }
}
