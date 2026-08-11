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
package com.hivemq.edge.adapters.opcua.config.tag;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * What a control event carries: the fields of {@code BaseEventType}, and nothing else.
 * <p>
 * The four events a refresh tag publishes — {@code RefreshStartEventType}, {@code RefreshEndEventType},
 * {@code RefreshRequiredEventType} and {@code EventQueueOverflowEventType} — all derive from
 * {@code BaseEventType} directly. None of them is a condition, so none carries {@code BranchId},
 * {@code Comment}, {@code ConditionClassId}, {@code ConditionName}, {@code EnabledState}, {@code Quality},
 * {@code Retain} or any of the other members {@code ConditionType} declares.
 * <p>
 * A refresh tag used to answer {@code ConditionType} to the question "what do you publish". The v01 pass
 * chose that to settle a three-way drift between the select clause, the schema and the decoder, and it did
 * settle it — but it settled it on a shape the events cannot fill. Thirteen condition fields were requested
 * from the server, decoded as null, and <em>advertised in the schema</em>, so a schema-aware consumer was
 * told to expect a condition-event contract from a control-event stream. The code comments beside it said
 * "select the base event fields" throughout, which is what this now is.
 * <p>
 * It costs nothing to be honest here, because the base fields were always the ones being filled. What changes
 * is what is asked for and what is promised: {@code unavailableFields} no longer has type-inapplicable
 * selections to report as server failures, and a generated model no longer carries thirteen keys that are
 * permanently null.
 * <p>
 * <b>{@code ConditionId} stays.</b> It is one of {@link OpcuaConditionType#BASE_EVENT_FIELDS} rather than a
 * condition-type member — the event's own node id, selected with an empty browse path — and the positional
 * decoding depends on that list being the prefix of every select clause Edge builds, whatever the tag.
 */
public enum BaseEventFieldSet implements EventFieldSet {

    /** The one instance; this describes a fixed set of fields, not a configurable one. */
    INSTANCE;

    @Override
    public @NotNull String browseName() {
        return "BaseEventType";
    }

    @Override
    public @NotNull List<String> allFields() {
        return OpcuaConditionType.BASE_EVENT_FIELDS;
    }

    @Override
    public @NotNull List<OpcuaConditionType.SelectedField> selectedFields() {
        return OpcuaConditionType.selectClauseFor(OpcuaConditionType.BASE_EVENT_FIELDS);
    }
}
