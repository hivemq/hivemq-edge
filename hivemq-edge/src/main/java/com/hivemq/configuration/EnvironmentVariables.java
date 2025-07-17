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
package com.hivemq.configuration;

/**
 * Provides names of environment variables for configuring HiveMQ folders.
 *
 * @author Silvio Giebl
 */
public class EnvironmentVariables {

    /**
     * Name of the environment variable for configuring the HiveMQ home folder.
     */
    public static final String HIVEMQ_HOME = "HIVEMQ_HOME";

    /**
     * Name of the environment variable for configuring the log folder.
     */
    public static final String LOG_FOLDER = "HIVEMQ_LOG_FOLDER";

    /**
     * Name of the environment variable for configuring the license folder.
     */
    public static final String LICENSE_FOLDER = "HIVEMQ_LICENSE_FOLDER";


    /**
     * Name of the environment variable for configuring the config folder.
     */
    public static final String CONFIG_FOLDER = "HIVEMQ_CONFIG_FOLDER";

    /**
     * Name of the environment variable for configuring the config folder for secondary configs like topicfilters.
     */
    public static final String CONFIG_FOLDER_SECONDARY = "HIVEMQ_CONFIG_SECONDARY";

    /**
     * Name of the environment variable for activating container mode
     */
    public static final String CONFIG_REFRESH_INTERVAL = "HIVEMQ_CONFIG_REFRESHINTERVAL";

    /**
     * Name of the environment variable for indicating a writeable config
     */
    public static final String CONFIG_WRITEABLE = "HIVEMQ_CONFIG_WRITEABLE";

    /**
     * Name of the environment variable for indicating that the config fragment will be zipped and base64 encoded
     */
    public static final String CONFIG_FRAGMENT_BASE64ZIP = "HIVEMQ_CONFIG_FRAGMENT_BASE64ZIP";

    /**
     * Name of the environment variable for configuring the data folder.
     */
    public static final String DATA_FOLDER = "HIVEMQ_DATA_FOLDER";

    /**
     * Name of the environment variable for configuring the extension folder.
     */
    public static final String EXTENSION_FOLDER = "HIVEMQ_EXTENSION_FOLDER";

    /**
     * Name of the environment variable for configuring the modules folder.
     */
    public static final String MODULES_FOLDER = "HIVEMQ_MODULES_FOLDER";

}
