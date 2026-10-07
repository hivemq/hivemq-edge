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
package com.hivemq.protocols.tag;

import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import org.jetbrains.annotations.NotNull;

public class TagSchemaCreationInputImpl implements TagSchemaCreationInput {

    private final @NotNull String tagName;
    private final boolean methodCalls;
    private final boolean correlationData;
    private final boolean responseTopic;

    /**
     * For schema generation with no mapping behind it — discovery, browsing, the REST tag-schema endpoint.
     * <p>
     * All three mapping flags are false, which is what "no mapping asked for anything" means: the generated
     * southbound schema then requires none of the request fields.
     */
    public TagSchemaCreationInputImpl(final @NotNull String tagName) {
        this(tagName, false, false, false);
    }

    /**
     * For schema generation on behalf of one southbound mapping, carrying what that mapping asked to read.
     */
    public TagSchemaCreationInputImpl(
            final @NotNull String tagName,
            final boolean methodCalls,
            final boolean correlationData,
            final boolean responseTopic) {
        this.tagName = tagName;
        this.methodCalls = methodCalls;
        this.correlationData = correlationData;
        this.responseTopic = responseTopic;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public boolean isMethodCalls() {
        return methodCalls;
    }

    @Override
    public boolean isCorrelationData() {
        return correlationData;
    }

    @Override
    public boolean isResponseTopic() {
        return responseTopic;
    }
}
