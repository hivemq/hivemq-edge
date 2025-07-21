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
package com.hivemq.configuration.info;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Useful information about HiveMQ and the underlying system
 *
 * @author Christoph Schäbel
 * @since 3.0
 */
public interface SystemInformation {

    /**
     * Sets home folder and HiveMQ version.
     */
    void init();

    /**
     * @return the version string of HiveMQ
     */
    @NotNull
    String getHiveMQVersion();

    /**
     * @return the home folder of HiveMQ
     */
    @NotNull
    File getHiveMQHomeFolder();

    /**
     * @return a secondary folder where additional configs like topic filters are stored
     */
    @NotNull
    File getSecondaryHiveMQHomeFolder();

    /**
     * /**
     *
     * @return the config folder of HiveMQ
     */
    @NotNull
    File getConfigFolder();

    /**
     * @return the log folder of HiveMQ
     */
    @NotNull
    File getLogFolder();

    /**
     * @return the data folder of HiveMQ
     */
    @NotNull
    File getDataFolder();

    /**
     * /**
     *
     * @return the config folder of HiveMQ
     */
    @NotNull
    File getLicenseFolder();

    /**
     * @return the extensions folder of HiveMQ
     */
    @NotNull
    File getExtensionsFolder();

    /**
     * @return the modules folder of HiveMQ
     */
    @NotNull File getModulesFolder();

    /**
     * @return the timestamp of HiveMQ start
     */
    long getRunningSince();

    /**
     * @return the count of CPUs HiveMQ uses
     */
    int getProcessorCount();

    /**
     * @return is HiveMQ running in embedded mode
     */
    boolean isEmbedded();

    /**
     * @return should the fragment config be treated as bing zipped and base64 encoded
     */
    boolean isConfigFragmentBase64Zip();

    /**
     * @return the interval between refreshing config files, 0 means no refreshing
     */
    long configRefreshIntervalInMs();

    /**
     * Indicates whether the config can be written
     *
     * @return false if the config can't be written.
     */
    boolean isConfigWriteable();
}
