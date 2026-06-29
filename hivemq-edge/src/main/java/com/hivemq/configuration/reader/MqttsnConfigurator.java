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

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.MqttSnConfigEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT-SN support has been removed from HiveMQ Edge. The configuration schema is still parsed for backwards
 * compatibility, but the protocol is no longer served. This configurator does not apply any configuration; it only
 * warns operators that any remaining {@code <mqtt-sn>} / {@code <mqtt-sn-listeners>} configuration is obsolete:
 * <ul>
 *     <li>an <b>ERROR</b> is logged when MQTT-SN is activated (a {@code <mqtt-sn-listeners>} UDP listener is
 *     configured), because that listener will not be started and no MQTT-SN traffic will be served;</li>
 *     <li>a <b>WARN</b> is logged when only the {@code <mqtt-sn>} block is present (MQTT-SN is configured but not
 *     activated), announcing that the obsolete block will be removed in a future release.</li>
 * </ul>
 */
public class MqttsnConfigurator implements Configurator<MqttSnConfigEntity> {

    private static final Logger log = LoggerFactory.getLogger(MqttsnConfigurator.class);

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        final boolean activated = !config.getMqttsnListenerConfig().isEmpty();
        final boolean blockPresent = config.getMqttsnConfig().isPresent();

        if (activated) {
            log.error(
                    "MQTT-SN is no longer supported by HiveMQ Edge. The configured 'mqtt-sn-listeners' will be ignored "
                            + "and no MQTT-SN traffic will be served. Please remove the 'mqtt-sn-listeners' and "
                            + "'mqtt-sn' configuration blocks.");
        } else if (blockPresent) {
            log.warn("The 'mqtt-sn' configuration block is deprecated and will be removed in a future release. "
                    + "MQTT-SN is no longer supported by HiveMQ Edge.");
        }

        return ConfigResult.SUCCESS;
    }
}
