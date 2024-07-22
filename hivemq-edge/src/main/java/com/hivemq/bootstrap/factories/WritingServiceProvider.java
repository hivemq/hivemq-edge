package com.hivemq.bootstrap.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.protocols.writing.NanoTimeProvider;
import com.hivemq.protocols.writing.ProtocolAdapterWritingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WritingServiceProvider {

    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull SingleWriterService singleWriterService;

    @Inject
    public WritingServiceProvider(
            final @NotNull EdgeCoreFactoryService edgeCoreFactoryService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull NanoTimeProvider nanoTimeProvider,
            final @NotNull SingleWriterService singleWriterService) {
        this.edgeCoreFactoryService = edgeCoreFactoryService;
        this.objectMapper = objectMapper;
        this.localTopicTree = localTopicTree;
        this.nanoTimeProvider = nanoTimeProvider;
        this.singleWriterService = singleWriterService;
    }


    public @NotNull ProtocolAdapterWritingService get() {
        final WritingServiceFactory writingServiceFactory = edgeCoreFactoryService.getWritingServiceFactory();
        if (writingServiceFactory == null) {
            return new WritingServiceNoop();
        }
        return writingServiceFactory.build(objectMapper, localTopicTree, nanoTimeProvider, singleWriterService);
    }


    public static class WritingServiceNoop implements ProtocolAdapterWritingService {

        private static final @NotNull Logger log = LoggerFactory.getLogger(WritingServiceNoop.class);

        @Override
        public void startWriting(@NotNull final WritingProtocolAdapter<?, ?> writingProtocolAdapter) {
            log.warn("No bidirectional module is currently installed. Writing to PLCs is currently not supported.");
        }
    }
}
