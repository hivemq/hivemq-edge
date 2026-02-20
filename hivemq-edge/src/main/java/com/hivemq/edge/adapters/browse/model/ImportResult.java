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
package com.hivemq.edge.adapters.browse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Result of a successful bulk tag import operation.
 *
 * @param tagsCreated               count of new tags created
 * @param tagsUpdated               count of existing tags overwritten
 * @param tagsDeleted               count of tags removed
 * @param northboundMappingsCreated count of northbound mappings created
 * @param northboundMappingsDeleted count of northbound mappings removed
 * @param southboundMappingsCreated count of southbound mappings created
 * @param southboundMappingsDeleted count of southbound mappings removed
 * @param tagActions                per-tag detail of what was done
 */
public record ImportResult(
        @JsonProperty("tagsCreated") int tagsCreated,
        @JsonProperty("tagsUpdated") int tagsUpdated,
        @JsonProperty("tagsDeleted") int tagsDeleted,
        @JsonProperty("northboundMappingsCreated") int northboundMappingsCreated,
        @JsonProperty("northboundMappingsDeleted") int northboundMappingsDeleted,
        @JsonProperty("southboundMappingsCreated") int southboundMappingsCreated,
        @JsonProperty("southboundMappingsDeleted") int southboundMappingsDeleted,
        @JsonProperty("tagActions") @NotNull List<TagAction> tagActions) {}
