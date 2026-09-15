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
 * The set of fields a tag publishes — and therefore the select clause asked of the server, the schema
 * advertised to consumers, and the list the decoder walks positionally.
 * <p>
 * Those four have to be one thing. The decoder matches values to names by <em>position</em> against the select
 * clause, so a field present in one list and absent from another does not produce a missing key: it shifts
 * every field after it onto the wrong name. The v01 pass found the four had drifted into three different
 * answers and funnelled them through a single accessor on the tag definition; this interface is what that
 * accessor returns.
 * <p>
 * Two implementations, because two genuinely different kinds of event reach Edge.
 * <ul>
 *   <li>{@link OpcuaConditionType} — a condition or alarm, whose fields follow from its declared type.</li>
 *   <li>{@link BaseEventFieldSet} — the control events, which derive from {@code BaseEventType} and carry
 *       nothing a condition type declares.</li>
 * </ul>
 * A refresh tag used to answer {@code ConditionType} here, which was convenient rather than true: see
 * {@link BaseEventFieldSet} for what that cost.
 */
public sealed interface EventFieldSet permits OpcuaConditionType, BaseEventFieldSet {

    /** The OPC UA type whose fields these are, for logs and diagnostics. */
    @NotNull
    String browseName();

    /** Every field an event of this shape carries, in the order the published object reads. */
    @NotNull
    List<String> allFields();

    /**
     * Every field to select, in the order the select clause and the decoder both walk — {@link #allFields()}
     * with an extra entry after each field that carries an {@code Id} companion.
     */
    @NotNull
    List<OpcuaConditionType.SelectedField> selectedFields();
}
