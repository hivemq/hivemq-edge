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
package com.hivemq.configuration.migration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that runs configuration migrations at startup.
 * <p>
 * This service is instantiated as an eager singleton to ensure migrations run
 * after all extractors are initialized but before the system starts processing.
 */
@Singleton
public class ConfigMigrationService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigMigrationService.class);

    @Inject
    public ConfigMigrationService(final @NotNull DataCombiningScopeMigrator dataCombiningScopeMigrator) {
        log.debug("Running configuration migrations at startup...");
        dataCombiningScopeMigrator.migrateUnscopedTags();
        log.debug("Configuration migrations completed.");
    }
}
