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
package com.hivemq.logging.modifier;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Marker;

public class MiscLogLevelModifier implements LogLevelModifier {

    @Override
    public @NotNull FilterReply decide(
            final @Nullable Marker marker,
            final @NotNull Logger logger,
            final @NotNull Level level,
            final @NotNull String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        if (format == null || logger == null) {
            return FilterReply.NEUTRAL;
        }

        if (logger.getName() == null) {
            return FilterReply.NEUTRAL;
        }

        if (level == Level.WARN) {
            if (logger.getName().equals("org.glassfish.jersey.internal.inject.Providers")) {
                logger.trace(marker, format, params);
                return FilterReply.DENY;
            }
        }

        if (level == Level.DEBUG) {
            if (logger.getName().startsWith("com.github.victools.jsonschema") ||
                    logger.getName().startsWith("org.jose4j")) {
                //Actually we don't want this at all its far too noisy
//                logger.trace(marker, format, params);
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}
