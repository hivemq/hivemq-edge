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

import com.hivemq.HiveMQEdgeMain;
import com.hivemq.configuration.EnvironmentVariables;
import com.hivemq.configuration.SystemProperties;
import com.hivemq.util.ManifestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christoph Schäbel
 */
@ExtendWith(SystemStubsExtension.class)
public class SystemInformationImplTest {

    @TempDir
    File tempFolder;

    private SystemInformation systemInformation;


    @Test
    public void test_getHiveMQVersion() {
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        //check if there is a manifest file present (happens on jenkins) and use the value from the manifest file
        final String valueFromManifest = ManifestUtils.getValueFromManifest(HiveMQEdgeMain.class, "HiveMQ-Edge-Version");

        assertThat(Objects.requireNonNullElse(valueFromManifest, "Development Snapshot"))
                .isEqualTo(systemInformation.getHiveMQVersion());

    }

    @Test
    public void test_getHiveMQVersion_from_system_information_with_path() {
        systemInformation = new SystemInformationImpl(true);
        systemInformation.init();

        //check if there is a manifest file present (happens on jenkins) and use the value from the manifest file
        final String valueFromManifest = ManifestUtils.getValueFromManifest(HiveMQEdgeMain.class, "HiveMQ-Edge-Version");

        assertThat(Objects.requireNonNullElse(valueFromManifest, "Development Snapshot"))
                .isEqualTo(systemInformation.getHiveMQVersion());
    }

    @Test
    public void test_getHiveMQHomeFolder_from_system_information_with_path(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        systemProperties.set(SystemProperties.HIVEMQ_HOME, tempFolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(tempFolder.getAbsolutePath()).isEqualTo(systemInformation.getHiveMQHomeFolder().getAbsolutePath());
    }

    @Test
    public void test_getHiveMQHomeFolder_environmentVariable(final uk.org.webcompere.systemstubs.environment.EnvironmentVariables environmentVariables) {
        final var testfolder = new File(tempFolder, "home");

        environmentVariables.set(EnvironmentVariables.HIVEMQ_HOME, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath()).isEqualTo(systemInformation.getHiveMQHomeFolder().getAbsolutePath());
    }

    @Test
    public void test_getConfigFolder_default(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        systemProperties.set(SystemProperties.HIVEMQ_HOME, tempFolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(tempFolder.getAbsolutePath() + File.separator + "conf").isEqualTo(systemInformation.getConfigFolder().getAbsolutePath());
    }

    @Test
    public void test_getConfigFolder_property(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        final var testfolder = new File(tempFolder, "testconfig");

        systemProperties.set(SystemProperties.CONFIG_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath()).isEqualTo(systemInformation.getConfigFolder().getAbsolutePath());
    }

    @Test
    public void test_getConfigFolder_environmentVariable(final uk.org.webcompere.systemstubs.environment.EnvironmentVariables environmentVariables) {
        final var testfolder = new File(tempFolder, "testconfig");

        environmentVariables.set(EnvironmentVariables.CONFIG_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath()).isEqualTo(systemInformation.getConfigFolder().getAbsolutePath());
    }

    @Test
    public void test_getLogFolder_default(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        systemProperties.set(SystemProperties.HIVEMQ_HOME, tempFolder.getAbsolutePath());
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(tempFolder.getAbsolutePath() + File.separator + "log").isEqualTo(systemInformation.getLogFolder().getAbsolutePath());
    }

    @Test
    public void test_getLogFolder_property(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        final var testfolder = new File(tempFolder, "testlogs");

        systemProperties.set(SystemProperties.LOG_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath()).isEqualTo(systemInformation.getLogFolder().getAbsolutePath());
    }

    @Test
    public void test_getLogFolder_environmentVariable(final uk.org.webcompere.systemstubs.environment.EnvironmentVariables environmentVariables) {
        final var testfolder = new File(tempFolder, "testlogs");

        environmentVariables.set(EnvironmentVariables.LOG_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath()).isEqualTo(systemInformation.getLogFolder().getAbsolutePath());
    }

    @Test
    public void test_getDataFolder_default(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        systemProperties.set(SystemProperties.HIVEMQ_HOME, tempFolder.getAbsolutePath());
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(tempFolder.getAbsolutePath() + File.separator + "data")
                .isEqualTo(systemInformation.getDataFolder().getAbsolutePath());
    }

    @Test
    public void test_getDataFolder_property(final uk.org.webcompere.systemstubs.properties.SystemProperties systemProperties) {
        final var testfolder = new File(tempFolder, "testdatas");

        systemProperties.set(SystemProperties.DATA_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath())
                .isEqualTo(systemInformation.getDataFolder().getAbsolutePath());
    }

    @Test
    public void test_getDataFolder_environmentVariable(final uk.org.webcompere.systemstubs.environment.EnvironmentVariables environmentVariables) {
        final var testfolder = new File(tempFolder, "testdatas");

        environmentVariables.set(EnvironmentVariables.DATA_FOLDER, testfolder.getAbsolutePath());

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(testfolder.getAbsolutePath())
                .isEqualTo(systemInformation.getDataFolder().getAbsolutePath());
    }

    @Test
    public void test_create_plugin_folder_if_not_exists() {

        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(systemInformation.getExtensionsFolder().exists())
                .isTrue();
    }

    @Test
    public void test_create_data_folder_if_not_exists() {
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(systemInformation.getDataFolder().exists())
                .isTrue();
    }

    @Test
    public void test_create_log_folder_if_not_exists(){
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(systemInformation.getLogFolder().exists())
                .isTrue();
    }

    @Test
    public void test_get_core_count() {
        systemInformation = new SystemInformationImpl();
        systemInformation.init();

        assertThat(systemInformation.getProcessorCount())
                .isGreaterThan(0);
    }

}
