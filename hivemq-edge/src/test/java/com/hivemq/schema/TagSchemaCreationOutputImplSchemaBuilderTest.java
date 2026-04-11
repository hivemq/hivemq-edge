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
package com.hivemq.schema;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// TODO: these tests appear to be duplicates of SchemaBuilderImplTest and do not actually test
// TagSchemaCreationOutputImpl behaviour (finish, notSupported, adapterNotStarted, fail,
// tagNotFound, status transitions, future completion). They also rely on a tagSchemaBuilder()
// method that was never implemented. The whole class should be replaced with real tests for
// TagSchemaCreationOutputImpl.
@Disabled("tagSchemaBuilder() was never implemented; these tests are duplicates of SchemaBuilderImplTest")
class TagSchemaCreationOutputImplSchemaBuilderTest {

    @Test
    void test_tagSchemaBuilder_build_completesTheFuture() {
        // TODO: rewrite as a real test for TagSchemaCreationOutputImpl
    }

    @Test
    void test_tagSchemaBuilder_object_completesWithJsonSchema() {
        // TODO: rewrite as a real test for TagSchemaCreationOutputImpl
    }

    @Test
    void test_tagSchemaBuilder_buildReturnsSchemaObject() {
        // TODO: rewrite as a real test for TagSchemaCreationOutputImpl
    }

    @Test
    void test_tagSchemaBuilder_statusRemainsSuccess() {
        // TODO: rewrite as a real test for TagSchemaCreationOutputImpl
    }
}
