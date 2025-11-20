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
package com.hivemq.migration.meta;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
// MANUAL: import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class MetaFileServiceTest {

    @TempDir
    public File temporaryFolder;

    @Mock
    private SystemInformation systemInformation;

    private File dataFolder;
    @BeforeEach
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        dataFolder = temporaryFolder;
        when(systemInformation.getDataFolder()).thenReturn(dataFolder);
    }

    @Test
    public void test_read_file_data_folder_not_present() throws Exception {

        when(systemInformation.getDataFolder()).thenReturn(new File(RandomStringUtils.random(123)));

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        assertFalse(metaFile.isDataFolderPresent());
    }

    @Test
    public void test_read_file_persistence_folder_not_present() throws Exception {

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        assertTrue(metaFile.isDataFolderPresent());
        assertFalse(metaFile.isPersistenceFolderPresent());
    }

    @Test
    public void test_read_file_meta_file_not_present() throws Exception {

        new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME).mkdir();

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        assertTrue(metaFile.isDataFolderPresent());
        assertTrue(metaFile.isPersistenceFolderPresent());
        assertFalse(metaFile.isMetaFilePresent());
    }

    @Test
    public void test_read_write_meta_file() throws Exception {

        new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME).mkdir();

        final MetaInformation metaInformation = new MetaInformation();
        metaInformation.setHivemqVersion("1.2.3");
        metaInformation.setSubscriptionPersistenceVersion("1.2.0");
        metaInformation.setRetainedMessagesPersistenceVersion("1.2.1");
        metaInformation.setQueuedMessagesPersistenceVersion("20.1.3");
        metaInformation.setClientSessionPersistenceVersion("4.3.20");
        metaInformation.setPublishPayloadPersistenceVersion("6.3.33");
        metaInformation.setPublishPayloadPersistenceType(PersistenceMode.FILE);
        metaInformation.setRetainedMessagesPersistenceType(PersistenceMode.FILE_NATIVE);

        MetaFileService.writeMetaFile(systemInformation, metaInformation);

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        assertTrue(metaFile.isDataFolderPresent());
        assertTrue(metaFile.isPersistenceFolderPresent());
        assertTrue(metaFile.isMetaFilePresent());
        assertEquals("1.2.3", metaFile.getHivemqVersion());
        assertEquals("1.2.0", metaFile.getSubscriptionPersistenceVersion());
        assertEquals("1.2.1", metaFile.getRetainedMessagesPersistenceVersion());
        assertEquals("20.1.3", metaFile.getQueuedMessagesPersistenceVersion());
        assertEquals("4.3.20", metaFile.getClientSessionPersistenceVersion());
        assertEquals("6.3.33", metaFile.getPublishPayloadPersistenceVersion());
        assertEquals(PersistenceMode.FILE, metaFile.getPublishPayloadPersistenceType());
        assertEquals(PersistenceMode.FILE_NATIVE, metaFile.getRetainedMessagesPersistenceType());
    }
}
