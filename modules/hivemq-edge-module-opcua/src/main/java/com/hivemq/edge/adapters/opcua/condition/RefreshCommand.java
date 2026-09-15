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
package com.hivemq.edge.adapters.opcua.condition;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A request to refresh the current alarm picture — what a southbound write to a refresh tag means.
 * <p>
 * Deliberately shaped like {@link ConditionUpdate}: one required {@code method} field naming the action, so a
 * reader who has seen one command has seen both. It carries no other field because {@code ConditionRefresh}
 * takes no argument a user could supply — its only parameter is the subscription id, which Edge knows and the
 * user does not.
 */
public final class RefreshCommand {

    private RefreshCommand() {}

    public static final @NotNull String FIELD_METHOD = "method";

    /** The only defined action: re-report every retained condition on this adapter's subscription. */
    public static final @NotNull String METHOD_REFRESH = "REFRESH";

    /**
     * Checks that a payload is a well-formed refresh command.
     * <p>
     * There is nothing to return — the command has no parameters — so this validates and throws rather than
     * producing a value. Rejecting an unrecognised method matters more here than it might seem: a user who
     * writes {@code ACKNOWLEDGE} to a refresh tag has confused two tags, and silently refreshing instead of
     * acknowledging would be a worse answer than an error.
     *
     * @param payload the write payload's value.
     * @throws IllegalArgumentException if the payload is not an object, names no method, or names one that is
     *                                  not defined.
     */
    public static void validate(final @Nullable JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException(
                    "A refresh command must be an object with a '" + FIELD_METHOD + "' field");
        }
        final JsonNode methodNode = payload.get(FIELD_METHOD);
        if (methodNode == null || methodNode.isNull() || !methodNode.isTextual()) {
            throw new IllegalArgumentException("A refresh command needs a '" + FIELD_METHOD + "' field");
        }
        final String method = methodNode.asText();
        if (!METHOD_REFRESH.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(
                    "Unknown refresh method '" + method + "'. The only defined method is '" + METHOD_REFRESH + "'");
        }
    }
}
