package com.hivemq.protocols.writing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.writing.WriteInput;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class WriteTask {

    private final @NotNull WritingProtocolAdapter protocolAdapter;
    private final @NotNull ObjectMapper objectMapper;
    private final AtomicInteger counter = new AtomicInteger();

    WriteTask(
            final @NotNull WritingProtocolAdapter protocolAdapter, final @NotNull ObjectMapper objectMapper) {
        this.protocolAdapter = protocolAdapter;
        this.objectMapper = objectMapper;
    }

    public void onMessage(byte[] payload) throws JsonProcessingException {
        try {
            final Class<? extends WritePayload> payloadClass = protocolAdapter.getPayloadClass();
            final WritePayload writePayload = objectMapper.readValue(payload, payloadClass);
            final WriteInput writeInput = new WriteInputImpl<>(writePayload);
            final WriteOutputImpl writeOutput = new WriteOutputImpl();
            writeOutput.getFuture().whenComplete(((aBoolean, throwable) -> {
                System.err.println("throwable:" + throwable);
                System.err.println("booleam:" + aBoolean);
            }));
            protocolAdapter.write(writeInput, writeOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}
