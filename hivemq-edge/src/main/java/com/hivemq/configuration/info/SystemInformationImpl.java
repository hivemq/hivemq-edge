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

import com.google.common.io.Files;
import com.hivemq.HiveMQEdgeGateway;
import com.hivemq.HiveMQEdgeMain;
import com.hivemq.configuration.EnvironmentVariables;
import com.hivemq.configuration.SystemProperties;
import com.hivemq.util.ManifestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
public class SystemInformationImpl implements SystemInformation {

    private static final Logger log = LoggerFactory.getLogger(SystemInformationImpl.class);
    public static final String DEVELOPMENT_VERSION = "Development Snapshot";

    private @NotNull File homeFolder;
    private @NotNull File configFolder;
    private @NotNull File secondaryConfigFolder;
    private @NotNull File logFolder;
    private @NotNull File licenseFolder;
    private @NotNull File dataFolder;
    private @NotNull File pluginFolder;
    private @NotNull File modulesFolder;
    private @NotNull File pulseTokenFolder;
    private @NotNull String hivemqVersion;

    private final long runningSince;
    private final boolean embedded;
    private final int processorCount;

    private final long configRefreshIntervalInMs;

    private final boolean usePathOfRunningJar;

    private final boolean configWriteable;

    private final boolean configFragmentBase64Zip;

    public SystemInformationImpl() {
        this(false);
    }

    public SystemInformationImpl(final boolean usePathOfRunningJar) {
        this(usePathOfRunningJar, false, null, null, null, null, null, null, null);
    }

    public SystemInformationImpl(
            final boolean usePathOfRunningJar,
            final boolean embedded,
            final @Nullable File configFolder,
            final @Nullable File secondaryConfigFolder,
            final @Nullable File dataFolder,
            final @Nullable File pluginFolder,
            final @Nullable File modulesFolder,
            final @Nullable File licenseFolder,
            final @Nullable File pulseTokenLocation) {
        final String refreshInterval = getSystemPropertyOrEnvironmentVariable(SystemProperties.CONFIG_REFRESH_INTERVAL, EnvironmentVariables.CONFIG_REFRESH_INTERVAL);
        final String configWriteable = getSystemPropertyOrEnvironmentVariable(SystemProperties.CONFIG_WRITEABLE, EnvironmentVariables.CONFIG_WRITEABLE);
        final String configFragmentBase64ZipString = getSystemPropertyOrEnvironmentVariable(SystemProperties.CONFIG_FRAGMENT_BASE64ZIP, EnvironmentVariables.CONFIG_FRAGMENT_BASE64ZIP);
        this.usePathOfRunningJar = usePathOfRunningJar;
        this.embedded = embedded;
        this.configFolder = configFolder;
        this.secondaryConfigFolder = secondaryConfigFolder;
        this.dataFolder = dataFolder;
        this.pluginFolder = pluginFolder;
        this.modulesFolder = modulesFolder;
        this.licenseFolder = licenseFolder;
        this.pulseTokenFolder = pulseTokenLocation;
        this.runningSince = System.currentTimeMillis();
        this.configRefreshIntervalInMs = Long.parseLong(Objects.requireNonNullElse(refreshInterval, "-1"));
        this.configWriteable = Boolean.parseBoolean((configWriteable == null || configWriteable.isEmpty()) ? "true" : configWriteable );
        this.configFragmentBase64Zip = Boolean.parseBoolean((configFragmentBase64ZipString == null || configFragmentBase64ZipString.isEmpty()) ? "false" : configFragmentBase64ZipString );
        processorCount = getPhysicalProcessorCount();
    }

    public void init() {
        setHivemqVersion();
        setFolders();
    }

    private int getPhysicalProcessorCount() {
        final int runtimeProcessorCount = Runtime.getRuntime().availableProcessors();
        return Math.min(1, runtimeProcessorCount);
    }

    private void setFolders() {
        setHomeFolder();
        configFolder = Objects.requireNonNullElseGet(
                configFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.CONFIG_FOLDER,
                        EnvironmentVariables.CONFIG_FOLDER,
                        "conf",
                        false
                )
        );

        final String secondaryConfigFolderName = getSystemPropertyOrEnvironmentVariable(
                SystemProperties.CONFIG_FOLDER_SECONDARY,
                EnvironmentVariables.CONFIG_FOLDER_SECONDARY);

        if (secondaryConfigFolderName == null) {
            secondaryConfigFolder = configFolder;
        } else {
            secondaryConfigFolder = Objects.requireNonNullElseGet(
                    configFolder,
                    () -> setUpHiveMQFolder(
                            SystemProperties.CONFIG_FOLDER_SECONDARY,
                            EnvironmentVariables.CONFIG_FOLDER_SECONDARY,
                            "conf",
                            false
                    )
            );
        }

        licenseFolder = Objects.requireNonNullElseGet(
                licenseFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.LICENSE_FOLDER,
                        EnvironmentVariables.LICENSE_FOLDER,
                        "license",
                        false
                )
        );


        logFolder = setUpHiveMQFolder(SystemProperties.LOG_FOLDER, EnvironmentVariables.LOG_FOLDER, "log", !embedded);
        // Set log folder property for logger-xml-config
        System.setProperty(SystemProperties.LOG_FOLDER, logFolder.getAbsolutePath());

        dataFolder = Objects.requireNonNullElseGet(
                dataFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.DATA_FOLDER,
                        EnvironmentVariables.DATA_FOLDER,
                        "data",
                        true
                )
        );

        pulseTokenFolder = Objects.requireNonNullElseGet(pulseTokenFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.PULSE_TOKEN_FOLDER,
                        EnvironmentVariables.PULSE_TOKEN_FOLDER,
                        new File(dataFolder, "pulsetoken").getAbsolutePath(),
                        true
                )
        );

        pluginFolder = Objects.requireNonNullElseGet(
                pluginFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.EXTENSIONS_FOLDER,
                        EnvironmentVariables.EXTENSION_FOLDER,
                        "extensions",
                        !embedded
                )
        );

        modulesFolder = Objects.requireNonNullElseGet(
                modulesFolder,
                () -> setUpHiveMQFolder(
                        SystemProperties.MODULES_FOLDER,
                        EnvironmentVariables.MODULES_FOLDER,
                        "modules",
                        !embedded
                )
        );
    }

    private void setHivemqVersion() {

        var hivemqVersion = ManifestUtils.getValueFromManifest(HiveMQEdgeMain.class, "HiveMQ-Edge-Version");

        if (hivemqVersion == null || hivemqVersion.isEmpty()) {
            hivemqVersion = DEVELOPMENT_VERSION;
        }

        this.hivemqVersion = hivemqVersion;

        log.info("HiveMQ Edge Version: {}", hivemqVersion);
    }

    public void setHivemqVersion(final @NotNull String hivemqVersion) {
        this.hivemqVersion = hivemqVersion;
    }

    @Override
    public @NotNull String getHiveMQVersion() {
        return hivemqVersion;
    }

    @Override
    public @NotNull File getHiveMQHomeFolder() {
        return homeFolder;
    }

    @Override
    public @NotNull File getConfigFolder() {
        return configFolder;
    }


    @Override
    public @NotNull File getSecondaryHiveMQHomeFolder() {
        return secondaryConfigFolder;
    }

    @Override
    public @NotNull File getLogFolder() {
        return logFolder;
    }

    @Override
    public @NotNull File getDataFolder() {
        return dataFolder;
    }

    @Override
    public @NotNull File getLicenseFolder() {
        return licenseFolder;
    }

    @Override
    public @NotNull File getExtensionsFolder() {
        return this.pluginFolder;
    }

    @Override
    public @NotNull File getModulesFolder() {
        return this.modulesFolder;
    }

    @Override
    public @NotNull File getPulseTokenFolder() {
        return this.pulseTokenFolder;
    }

    @Override
    public long getRunningSince() {
        return runningSince;
    }

    @Override
    public long configRefreshIntervalInMs() {
        return configRefreshIntervalInMs;
    }

    /**
     * Tries to find a file in the given absolute path or relative to the HiveMQ home folder
     *
     * @param fileLocation the absolute or relative path
     * @return a file
     */
    private @NotNull File findAbsoluteAndRelative(final String fileLocation) {
        final File file = new File(fileLocation);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(getHiveMQHomeFolder(), fileLocation);
        }
    }

    private @NotNull File setUpHiveMQFolder(
            final String propertyName,
            final String variableName,
            final String defaultFolder,
            final boolean createFolderIfNotExists) {
        final String folderName = getSystemPropertyOrEnvironmentVariable(propertyName, variableName);
        final File folder;
        if (folderName != null) {
            folder = findAbsoluteAndRelative(folderName);
        } else {
            folder = findAbsoluteAndRelative(defaultFolder);
        }

        if (createFolderIfNotExists && !folder.exists()) {
            final boolean mkdirsResult = folder.mkdirs();
            if (!mkdirsResult) {
                log.warn("Not able to create folder {}, HiveMQ will behave unexpectedly!", folder);
            }
        }

        return folder;
    }

    /**
     * Returns the value of the system property or environment variable prioritising the system property.
     *
     * @param propertyName the name of the system property
     * @param variableName the name of the environment variable
     * @return value of the system property, if not present value of the environment variable, if not present null
     */
    private @Nullable String getSystemPropertyOrEnvironmentVariable(
            final String propertyName, final String variableName) {
        final String systemProperty = System.getProperty(propertyName);
        if (systemProperty != null) {
            return systemProperty;
        }

        return System.getenv().get(variableName);
    }

    private void setHomeFolder() {
        final String home =
                getSystemPropertyOrEnvironmentVariable(SystemProperties.HIVEMQ_HOME, EnvironmentVariables.HIVEMQ_HOME);

        if (home != null) {
            homeFolder = findAbsoluteAndRelative(home);
            log.info("HiveMQ home directory: {}", homeFolder.getAbsolutePath());

            //setting system property to support the deprecated PathUtils in the SPI
            System.setProperty(SystemProperties.HIVEMQ_HOME, homeFolder.getAbsolutePath());

        } else if (usePathOfRunningJar) {
            usePathOfRunningJarAsHomeFolder();
        } else {
            useTemporaryHomeFolder();
        }
    }

    private void useTemporaryHomeFolder() {
        final File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        log.warn(
                "No {} property or {} environment variable was set. Using a temporary directory ({}) HiveMQ will behave unexpectedly!",
                SystemProperties.HIVEMQ_HOME,
                EnvironmentVariables.HIVEMQ_HOME,
                tempDir.getAbsolutePath());
        homeFolder = tempDir;
    }

    private void usePathOfRunningJarAsHomeFolder() {
        final File pathOfRunningJar = getPathOfRunningJar();
        if (!embedded) {
            log.warn("No {} property or {} environment variable was set. Using {}",
                    SystemProperties.HIVEMQ_HOME,
                    EnvironmentVariables.HIVEMQ_HOME,
                    pathOfRunningJar.getAbsolutePath());
        }
        homeFolder = pathOfRunningJar;
    }

    private @NotNull File getPathOfRunningJar() {
        final String decode =
                URLDecoder.decode(HiveMQEdgeGateway.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                        StandardCharsets.UTF_8);
        final String path = decode.substring(0, decode.lastIndexOf('/') + 1);
        return new File(path);
    }

    @Override
    public int getProcessorCount() {
        return this.processorCount;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    @Override
    public boolean isConfigWriteable() {
        return configWriteable;
    }

    @Override
    public boolean isConfigFragmentBase64Zip() {
        return configFragmentBase64Zip;
    }
}
