/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.etherip_cip_odva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.IntNode;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipWriteMode;
import com.hivemq.edge.adapters.etherip_cip_odva.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the input-validation guards of {@code write()} that reject a request before any device
 * interaction. The connected happy paths (scalar/composite COMPLETE_WRITE and PARTIAL_WRITE) are covered
 * end-to-end by the {@code hivemq-edge-test} integration tests against a real enip_server.
 */
class EthernetIPCipOdvaWriteTest {

    @Test
    void write_unknownTag_fails() {
        final var adapter = newAdapter(List.of(writableTag("known", "@22/1/1")));
        final CapturingWritingOutput output = new CapturingWritingOutput();

        adapter.write(writingInput("unknown", 1), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("unknown").contains("not found");
    }

    @Test
    void write_readOnlyTag_fails() {
        final var adapter = newAdapter(List.of(readOnlyTag("ro", "@22/1/1")));
        final CapturingWritingOutput output = new CapturingWritingOutput();

        adapter.write(writingInput("ro", 1), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("READ_ONLY");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static @NotNull WritingInput writingInput(final @NotNull String tagName, final int value) {
        final WritingContext context = mock(WritingContext.class);
        when(context.getTagName()).thenReturn(tagName);
        final WritingPayload payload = new CipWritePayload(new IntNode(value));
        final WritingInput input = mock(WritingInput.class);
        when(input.getWritingContext()).thenReturn(context);
        when(input.getWritingPayload()).thenReturn(payload);
        return input;
    }

    private static @NotNull CipTag writableTag(final @NotNull String name, final @NotNull String address) {
        return new CipTag(
                name,
                name,
                new CipTagDefinition(
                        address,
                        1,
                        CipDataType.INT,
                        0d,
                        null,
                        0,
                        null,
                        CipReadWrite.READ_WRITE,
                        CipWriteMode.COMPLETE_WRITE));
    }

    private static @NotNull CipTag readOnlyTag(final @NotNull String name, final @NotNull String address) {
        return new CipTag(
                name,
                name,
                new CipTagDefinition(address, 1, CipDataType.INT, 0d, null, 0, null, CipReadWrite.READ_ONLY, null));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull EthernetIPCipOdvaPollingProtocolAdapter newAdapter(final List<CipTag> tags) {
        final ProtocolAdapterInput<EipSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter");
        when(input.getTags()).thenReturn((List) tags);
        return new EthernetIPCipOdvaPollingProtocolAdapter(EthernetIPCipOdvaProtocolAdapterInformation.INSTANCE, input);
    }

    private static final class CapturingWritingOutput implements WritingOutput {
        boolean failed;

        @Nullable
        String failMessage;

        @Override
        public void finish() {}

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            this.failed = true;
            this.failMessage = errorMessage;
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            this.failed = true;
            this.failMessage = errorMessage;
        }
    }
}
