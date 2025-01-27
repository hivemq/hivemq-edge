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
package com.hivemq.util;

import com.hivemq.util.render.FileFragmentUtil;
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
        final FileFragmentUtil.FragmentResult resolved = FileFragmentUtil.replaceFragmentPlaceHolders(fragmented);

        assertThat(resolved.getRenderResult()).isEqualTo("<hallo>THE FRAGMENT</hallo>");
        assertThat(resolved.getFragmentToModificationTime()).containsKey(fragment);
    }
}
