/*
 * Copyright 2024-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.postgresql.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostgreSQLAdapterTagDefinition implements TagDefinition {
    @JsonProperty(value = "query", required = true)
    @ModuleConfigField(title = "Query",
            description = "Query to execute on the database",
            required = true,
            format = ModuleConfigField.FieldType.UNSPECIFIED)
    private final @Nullable String query;

    @JsonProperty(value = "rowLimit", required = true)
    @ModuleConfigField(title = "Row Limit",
            description = "Number of row to retrieve (default 10, maximum 99)",
            required = true,
            numberMin = 1,
            numberMax = 99)
    private final int rowLimit;

    @JsonProperty(value = "spiltLinesInIndividualMessages")
    @ModuleConfigField(title = "Split lines into individual messages ?",
            description = "Select this option to create a single message per line returned by the query (by default all lines are sent in a single message as an array).",
            defaultValue = "false")
    protected @NotNull Boolean spiltLinesInIndividualMessages;

    public PostgreSQLAdapterTagDefinition(
            @JsonProperty(value = "query") final @Nullable String query,
            @JsonProperty(value = "rowLimit") final @Nullable Integer rowLimit,
            @JsonProperty(value = "spiltLinesInIndividualMessages") final @Nullable Boolean spiltLinesInIndividualMessages){
        this.query = query;
        assert rowLimit != null;
        this.rowLimit = rowLimit;
        assert spiltLinesInIndividualMessages != null;
        this.spiltLinesInIndividualMessages = spiltLinesInIndividualMessages;
    }

    public @Nullable String getQuery(){return query;}

    public int getRowLimit() {return rowLimit;}

    public @NotNull Boolean getSpiltLinesInIndividualMessages() {return spiltLinesInIndividualMessages;}


}
