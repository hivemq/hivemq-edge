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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import org.jetbrains.annotations.NotNull;

// internal view on a writing context.
public interface InternalWritingContext extends WritingContext {

    @NotNull
    FieldMapping getFieldMapping();

    @NotNull
    String getSchema();

    /**
     * Whether messages on this mapping are method calls rather than value writes.
     * <p>
     * Decides whether the fields a method request carries beside {@code value} survive the southbound transform:
     * the field mapping is a whitelist that rebuilds the payload from nothing, so a field no instruction names is
     * dropped. These three flags are what add those instructions.
     */
    boolean isMethodCalls();

    /** Whether a request's correlation token is read, so the response can echo it. */
    boolean isCorrelationData();

    /** Whether a request may name the topic its response is published to. */
    boolean isResponseTopic();

    /**
     * A schema builder for this mapping's tag, with the request fields a method request carries already resolved.
     * <p>
     * A tag has one schema but may be reached by several mappings, so which fields that schema declares is a
     * question about all of them — not about this one. Resolving it here keeps the caller from having to know
     * that, and keeps the mapping list out of the caller's hands.
     */
    @NotNull
    TagSchemaCreationOutputImpl tagSchemaOutput();
}
