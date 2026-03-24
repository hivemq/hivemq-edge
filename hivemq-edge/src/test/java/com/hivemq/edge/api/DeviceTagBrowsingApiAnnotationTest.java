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
package com.hivemq.edge.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class DeviceTagBrowsingApiAnnotationTest {

    @Test
    void classHasPathAndTag() {
        assertThat(DeviceTagBrowsingApi.class.getAnnotation(Path.class)).isNotNull();
        assertThat(DeviceTagBrowsingApi.class.getAnnotation(Path.class).value()).contains("device-tags");

        assertThat(DeviceTagBrowsingApi.class.getAnnotation(Tag.class)).isNotNull();
        assertThat(DeviceTagBrowsingApi.class.getAnnotation(Tag.class).name()).isEqualTo("Protocol Adapters");
    }

    @Test
    void browseHasOperationAnnotation() throws NoSuchMethodException {
        final Method browse =
                DeviceTagBrowsingApi.class.getMethod("browse", String.class, String.class, int.class, String.class);
        final Operation op = browse.getAnnotation(Operation.class);
        assertThat(op).isNotNull();
        assertThat(op.operationId()).isEqualTo("browseDeviceTags");
        assertThat(op.responses()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void importTagsHasOperationAnnotation() throws NoSuchMethodException {
        final Method importTags = DeviceTagBrowsingApi.class.getMethod(
                "importTags", String.class, String.class, boolean.class, String.class, byte[].class);
        final Operation op = importTags.getAnnotation(Operation.class);
        assertThat(op).isNotNull();
        assertThat(op.operationId()).isEqualTo("importDeviceTags");
        assertThat(op.responses()).hasSizeGreaterThanOrEqualTo(3);
    }
}
