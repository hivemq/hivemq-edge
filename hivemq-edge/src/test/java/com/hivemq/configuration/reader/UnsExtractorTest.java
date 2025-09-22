package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.uns.config.ISA95;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UnsExtractorTest {

    @Test
    public void test_nullConfig_works() {
        final var configFileReaderWriter = mock(ConfigFileReaderWriter.class);
        final var extractor = new UnsExtractor(configFileReaderWriter);
        final var config = new HiveMQConfigEntity();
        extractor.sync(config);

        Assertions.assertThat(config.getUns().getIsa95().isEnabled()).isFalse();
        Assertions.assertThat(config.getUns().getIsa95().isPrefixAllTopics()).isFalse();
    }

    @Test
    public void test_config_works() {
        final var configFileReaderWriter = mock(ConfigFileReaderWriter.class);
        final var extractor = new UnsExtractor(configFileReaderWriter);
        final var isa95 = new ISA95(
                true,
                true,
                "enterprise",
                "site",
                "area",
                "line",
                "cell");
        final var config = new HiveMQConfigEntity();
        extractor.setISA95(isa95);
        extractor.sync(config);

        assertThat(config.getUns().getIsa95().isEnabled()).isTrue();
        assertThat(config.getUns().getIsa95().isPrefixAllTopics()).isTrue();
        assertThat(config.getUns().getIsa95().getArea()).isEqualTo(isa95.getArea());
        assertThat(config.getUns().getIsa95().getSite()).isEqualTo(isa95.getSite());
        assertThat(config.getUns().getIsa95().getWorkCell()).isEqualTo(isa95.getWorkCell());
        assertThat(config.getUns().getIsa95().getEnterprise()).isEqualTo(isa95.getEnterprise());
        assertThat(config.getUns().getIsa95().getProductionLine()).isEqualTo(isa95.getProductionLine());
    }

}
