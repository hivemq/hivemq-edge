/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols.fsm;

import com.hivemq.common.i18n.I18nError;
import com.hivemq.common.i18n.I18nErrorTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum I18nProtocolAdapterMessage implements I18nError {
    FSM_TRANSITION_FAILURE_UNABLE_TO_TRANSITION_FROM_STATE_TO_STATE,
    FSM_TRANSITION_SUCCESS_STATE_IS_UNCHANGED,
    FSM_TRANSITION_SUCCESS_TRANSITIONED_FROM_STATE_TO_STATE,
    PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_DELETED,
    PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND,
    ;

    private static final @NotNull String RESOURCE_NAME_PREFIX = "templates/protocol-adapter-messages-";
    private static final @NotNull String RESOURCE_NAME_SUFFIX = ".properties";
    private static final @NotNull I18nErrorTemplate TEMPLATE =
            new I18nErrorTemplate(locale -> RESOURCE_NAME_PREFIX + locale + RESOURCE_NAME_SUFFIX,
                    I18nProtocolAdapterMessage.class.getClassLoader());

    private final @NotNull String key;

    I18nProtocolAdapterMessage() {
        key = name().toLowerCase().replace("_", ".");
    }

    @Override
    public @NotNull String get(final @NotNull Map<String, Object> map) {
        return TEMPLATE.get(this, map);
    }

    @Override
    public @NotNull String getKey() {
        return key;
    }

    @Override
    public @NotNull String getName() {
        return name();
    }
}
