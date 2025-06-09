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
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.file.config.FileSpecificAdapterConfig;
import com.hivemq.edge.adapters.file.convertion.MappingException;
import com.hivemq.edge.adapters.file.payload.FileDataPoint;
import com.hivemq.edge.adapters.file.tag.FileTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class FilePollingProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull org.slf4j.Logger LOG = LoggerFactory.getLogger(FilePollingProtocolAdapter.class);

    private final @NotNull FileSpecificAdapterConfig adapterConfig;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<FileTag> tags;

    public FilePollingProtocolAdapter(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<FileSpecificAdapterConfig> input) {
        this.adapterId = adapterId;
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.tags = input.getTags().stream().map(tag -> (FileTag)tag).toList();
        this.protocolAdapterState = input.getProtocolAdapterState();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
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
            final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        String absolutePathToFle = "";
        try {
            for (final FileTag fileTag : tags) {
                absolutePathToFle = fileTag.getDefinition().getFilePath();
                final var path = Path.of(absolutePathToFle);
                final var length = path.toFile().length();
                final var limit = 64_000; // not a constant to have a more compact code example
                if (length > limit) {
                    pollingOutput.fail(String.format("File '%s' of size '%d' exceeds the limit '%d'.",
                            path.toAbsolutePath(),
                            length,
                            limit));
                    return;
                }
                final var fileContentArray = Files.readAllBytes(path);
                final var value = fileTag.getDefinition().getContentType().map(fileContentArray);
                pollingOutput.addDataPoint(new FileDataPoint(fileTag, value));
            }
        } catch (final IOException e) {
            LOG.warn("An exception occurred while reading the file '{}'.", absolutePathToFle, e);
            pollingOutput.fail(e, "An exception occurred while reading the file '" + absolutePathToFle + "'.");
        } catch (final MappingException e) {
            LOG.warn("An exception occurred while converting the data in file '{}' to a payload '{}'.",
                    absolutePathToFle,
                    e.getMessage());
            pollingOutput.fail(e, "An exception occurred while reading the file '" + absolutePathToFle + "'.");
        }
        pollingOutput.finish();
    }

    private static void pollFile(
            final @NotNull PollingOutput pollingOutput, final @NotNull FileTag fileTag) {

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
