/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.etherip_cip_odva.util;

import org.jetbrains.annotations.NotNull;

public class ExceptionUtils {
    private ExceptionUtils() {}

    public static String extractMessageWithCause(@NotNull Throwable e) {
        StringBuilder stringBuilder = new StringBuilder();

        extractMessageIfPresent(stringBuilder, e, "");
        extractMessageIfPresent(stringBuilder, e.getCause(), ". ");

        return stringBuilder.toString();
    }

    private static void extractMessageIfPresent(StringBuilder stringBuilder, Throwable e, String prefix) {
        if (e == null) {
            return;
        }
        stringBuilder.append(prefix).append(e.getClass().getSimpleName());

        if (e.getMessage() == null) {
            return;
        }

        stringBuilder.append(": ").append(e.getMessage());
    }
}
