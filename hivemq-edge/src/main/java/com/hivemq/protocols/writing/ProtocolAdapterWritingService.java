package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface ProtocolAdapterWritingService {
    String FORWARDER_PREFIX = "adapter-forwarder#";

    void startWriting(@NotNull WritingProtocolAdapter<?, ?> writingProtocolAdapter);
}
