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
 * @author Christoph Schäbel
 */
public class SystemProperties {

    public static final String HIVEMQ_HOME = "hivemq.home";

    public static final String LOG_FOLDER = "hivemq.log.folder";

    public static final String CONFIG_FOLDER = "hivemq.config.folder";
    public static final String CONFIG_FOLDER_SECONDARY = "hivemq.config.secondary";
    public static final String CONFIG_REFRESH_INTERVAL = "hivemq.config.refreshinterval";
    public static final String CONFIG_WRITEABLE = "hivemq.config.writeable";
    public static final String CONFIG_FRAGMENT_BASE64ZIP = "hivemq.config.fragment.base64zip";
    public static final String LICENSE_FOLDER = "hivemq.license.folder";

    public static final String DATA_FOLDER = "hivemq.data.folder";

    public static final String EXTENSIONS_FOLDER = "hivemq.extensions.folder";
    public static final String CORE_FOLDER = "hivemq.core.folder";

    public static final String MODULES_FOLDER = "hivemq.modules.folder";

    public static final String DIAGNOSTIC_MODE = "diagnosticMode";
}
