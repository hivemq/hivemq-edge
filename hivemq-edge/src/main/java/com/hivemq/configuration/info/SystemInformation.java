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

import java.io.File;
import org.jetbrains.annotations.NotNull;

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
     * Returns the version string of HiveMQ.
     *
     * @return the version string of HiveMQ
     */
    @NotNull
    String getHiveMQVersion();

    /**
     * Returns the home folder of HiveMQ.
     *
     * @return the home folder of HiveMQ
     */
    @NotNull
    File getHiveMQHomeFolder();

    /**
     * Returns a secondary folder where additional configs like topic filters are stored.
     *
     * @return a secondary folder where additional configs like topic filters are stored
     */
    @NotNull
    File getSecondaryHiveMQHomeFolder();

    /**
     * Returns the config folder of HiveMQ.
     *
     * @return the config folder of HiveMQ
     */
    @NotNull
    File getConfigFolder();

    /**
     * Returns the log folder of HiveMQ.
     *
     * @return the log folder of HiveMQ
     */
    @NotNull
    File getLogFolder();

    /**
     * Returns the data folder of HiveMQ.
     *
     * @return the data folder of HiveMQ
     */
    @NotNull
    File getDataFolder();

    /**
     * Returns the license folder of HiveMQ.
     *
     * @return the license folder of HiveMQ
     */
    @NotNull
    File getLicenseFolder();

    /**
     * Returns the extensions folder of HiveMQ.
     *
     * @return the extensions folder of HiveMQ
     */
    @NotNull
    File getExtensionsFolder();

    /**
     * Returns the folder where HiveMQ stores pulse related data.
     *
     * @return the folder where HiveMQ stores pulse related data
     */
    @NotNull
    File getPulseTokenFolder();

    /**
     * Returns the modules folder of HiveMQ.
     *
     * @return the modules folder of HiveMQ
     */
    @NotNull
    File getModulesFolder();

    /**
     * Returns the timestamp of HiveMQ start.
     *
     * @return the timestamp of HiveMQ start
     */
    long getRunningSince();

    /**
     * Returns the count of CPUs HiveMQ uses.
     *
     * @return the count of CPUs HiveMQ uses
     */
    int getProcessorCount();

    /**
     * Returns whether HiveMQ is running in embedded mode.
     *
     * @return is HiveMQ running in embedded mode
     */
    boolean isEmbedded();

    /**
     * Returns whether the fragment config should be treated as being zipped and base64 encoded.
     *
     * @return should the fragment config be treated as being zipped and base64 encoded
     */
    boolean isConfigFragmentBase64Zip();

    /**
     * Returns the interval between refreshing config files, 0 means no refreshing.
     *
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
