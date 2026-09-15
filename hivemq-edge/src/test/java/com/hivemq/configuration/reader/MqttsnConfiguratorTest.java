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
package com.hivemq.configuration.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.MqttSnConfigEntity;
import com.hivemq.configuration.entity.listener.ListenerEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class MqttsnConfiguratorTest {

    private final MqttsnConfigurator configurator = new MqttsnConfigurator();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(MqttsnConfigurator.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void applyConfig_whenMqttSnListenerConfigured_thenLogsError() {
        final HiveMQConfigEntity config = mock(HiveMQConfigEntity.class);
        when(config.getMqttsnListenerConfig()).thenReturn(List.of(mock(ListenerEntity.class)));
        when(config.getMqttsnConfig()).thenReturn(new MqttSnConfigEntity());

        configurator.applyConfig(config);

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR);
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.WARN);
    }

    @Test
    void applyConfig_whenOnlyMqttSnBlockPresent_thenLogsWarn() {
        final MqttSnConfigEntity present = mock(MqttSnConfigEntity.class);
        when(present.isPresent()).thenReturn(true);
        final HiveMQConfigEntity config = mock(HiveMQConfigEntity.class);
        when(config.getMqttsnListenerConfig()).thenReturn(List.of());
        when(config.getMqttsnConfig()).thenReturn(present);

        configurator.applyConfig(config);

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.WARN);
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void applyConfig_whenNoMqttSnConfig_thenLogsNothing() {
        final HiveMQConfigEntity config = mock(HiveMQConfigEntity.class);
        when(config.getMqttsnListenerConfig()).thenReturn(List.of());
        when(config.getMqttsnConfig()).thenReturn(new MqttSnConfigEntity());

        configurator.applyConfig(config);

        assertThat(appender.list).isEmpty();
    }

    @Test
    void mqttSnConfigEntity_isPresent_reflectsWhetherBlockWasUnmarshalled() throws Exception {
        // a defaulted entity (no XML) is not "present"
        assertThat(new MqttSnConfigEntity().isPresent()).isFalse();

        // an unmarshalled <mqtt-sn> block is "present"
        final JAXBContext ctx = JAXBContext.newInstance(MqttSnConfigEntity.class);
        final Unmarshaller unmarshaller = ctx.createUnmarshaller();
        final MqttSnConfigEntity unmarshalled = (MqttSnConfigEntity)
                unmarshaller.unmarshal(new StringReader("<mqtt-sn><gateway-id>1</gateway-id></mqtt-sn>"));
        assertThat(unmarshalled.isPresent()).isTrue();
    }
}
