/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.conformance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDG-737 — pure unit checks for the {@link ReferenceBrowseEngine}'s tag-name policy: the path→default-tag-name
 * derivation that the {@code browseName} carried on each browse entry now makes possible. No server needed.
 */
class ReferenceBrowseEngineTest {

    @Test
    void tagNameDefault_sanitizesEachPathSegment() {
        assertThat(ReferenceBrowseEngine.tagNameDefault("/Plant/Line 1/Temperature"))
                .isEqualTo("plant-line-1-temperature");
        assertThat(ReferenceBrowseEngine.tagNameDefault("/A/B/C")).isEqualTo("a-b-c");
        assertThat(ReferenceBrowseEngine.tagNameDefault("/shared")).isEqualTo("shared");
        assertThat(ReferenceBrowseEngine.tagNameDefault("")).isEmpty();
    }

    @Test
    void sanitize_collapsesAndTrimsNonAlphanumerics() {
        assertThat(ReferenceBrowseEngine.sanitize("Foo Bar Baz")).isEqualTo("foo-bar-baz");
        assertThat(ReferenceBrowseEngine.sanitize("--A__B--")).isEqualTo("a-b");
    }

    @Test
    void dedupDefaults_suffixesCollisions() {
        assertThat(ReferenceBrowseEngine.dedupDefaults(List.of("a", "a", "b", "a")))
                .containsExactly("a", "a-2", "b", "a-3");
    }
}
