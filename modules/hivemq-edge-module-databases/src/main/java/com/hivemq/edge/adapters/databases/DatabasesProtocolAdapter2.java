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
package com.hivemq.edge.adapters.databases;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2Bridge;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import org.jetbrains.annotations.NotNull;

/**
 * {@link com.hivemq.adapter.sdk.api.ProtocolAdapter2 ProtocolAdapter2} implementation for database adapters
 * (PostgreSQL, MySQL, MSSQL).
 * Northbound only (read from database, publish to MQTT).
 */
public class DatabasesProtocolAdapter2 extends ProtocolAdapter2Bridge {

    public DatabasesProtocolAdapter2(
            final @NotNull ProtocolAdapter delegate, final @NotNull ModuleServices moduleServices) {
        super(delegate, moduleServices);
    }

    @Override
    public boolean supportsSouthbound() {
        return false;
    }
}
