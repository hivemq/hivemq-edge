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
package com.hivemq.util.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileFragmentUtilTest {

    @Test
    public void testRender() throws Exception {
        final Path fragment = Files.createTempFile("fragment", ".xml");

        Files.writeString(fragment, "THE FRAGMENT");
        final String fragmented = "<hallo>${FRAGMENT:" + fragment.toFile() + "}</hallo>";
        final FileFragmentUtil.FragmentResult resolved = FileFragmentUtil.replaceFragmentPlaceHolders(fragmented, false);

        assertThat(resolved.getRenderResult()).isEqualTo("<hallo>THE FRAGMENT</hallo>");
        assertThat(resolved.getFragmentToModificationTime()).containsKey(fragment);
    }

    @Test
    public void testRender_zipped() throws Exception {
        final Path fragment = Files.createTempFile("fragment", ".xml");

        Files.writeString(fragment, "UEsDBAoAAAAAAHZc8VpKQ28aDQAAAA0AAAAHABwAdHN0LnR4dFVUCQAD8MN4aPHDeGh1eAsAAQT2AQAABBQAAABUSEUgRlJBR01FTlQKUEsBAh4DCgAAAAAAdlzxWkpDbxoNAAAADQAAAAcAGAAAAAAAAQAAAKSBAAAAAHRzdC50eHRVVAUAA/DDeGh1eAsAAQT2AQAABBQAAABQSwUGAAAAAAEAAQBNAAAATgAAAAAA");
        final String fragmented = "<hallo>${FRAGMENT:" + fragment.toFile() + "}</hallo>";
        final FileFragmentUtil.FragmentResult resolved = FileFragmentUtil.replaceFragmentPlaceHolders(fragmented, true);

        assertThat(resolved.getRenderResult()).isEqualTo("<hallo>THE FRAGMENT\n</hallo>");
        assertThat(resolved.getFragmentToModificationTime()).containsKey(fragment);
    }
}
