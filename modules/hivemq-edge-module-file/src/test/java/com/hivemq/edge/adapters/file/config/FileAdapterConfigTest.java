package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.file.FileProtocolAdapterFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FileAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final FileProtocolAdapterFactory fileProtocolAdapterFactory = new FileProtocolAdapterFactory();
        final FileAdapterConfig config =
                fileProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("file"));

        assertThat(config.getId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getFileToMqttConfig().getMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();
            assertThat(subscription.getFilePath()).isEqualTo("path/to/file1");
            assertThat(subscription.getContentType()).isEqualTo(ContentType.BINARY);

//            TODO: https://hivemq.kanbanize.com/ctrl_board/57/cards/24704/details/
//            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
//                assertThat(userProperty.getName()).isEqualTo("my-name");
//                assertThat(userProperty.getValue()).isEqualTo("my-value");
//            });
        }, subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(subscription.getQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

//            TODO: https://hivemq.kanbanize.com/ctrl_board/57/cards/24704/details/
//            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
//                assertThat(userProperty.getName()).isEqualTo("my-name");
//                assertThat(userProperty.getValue()).isEqualTo("my-value");
//            });

            assertThat(subscription.getFilePath()).isEqualTo("path/to/file2");
            assertThat(subscription.getContentType()).isEqualTo(ContentType.TEXT_CSV);
        });
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock());
        return readerWriter.applyConfig();
    }

}
