/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.etherip_cip_odva.exception;

import com.hivemq.edge.adapters.etherip_cip_odva.util.ExceptionUtils;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

public class ExceptionProcessor {
    private ExceptionProcessor() {}

    /**
     * Substitutes TimeOutException with a given substitute. Used to avoid issued with this piece:
     * java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable), because when TimeoutExceptiobn is thrown
     * it gets caught by: com.hivemq.edge.modules.adapters.impl.polling.PollingTask#handleInterruptionException(java.lang.Throwable) which is not correct/expected and will stop the adapter after 10 errors even if these were caught correctly by ProtocolAdapter implementation itself
     *
     * @param maybeTimeOutException maybe java.util.concurrent.TimeoutException
     * @param substitution operation used to substitute TimeoutException
     * @return substituted exception
     */
    public static Exception substituteTimeoutException(
            Exception maybeTimeOutException, UnaryOperator<Exception> substitution) {
        if (maybeTimeOutException instanceof TimeoutException) {
            return substitution.apply(maybeTimeOutException);
        }

        return maybeTimeOutException;
    }

    public static Exception substituteTimeOutExceptionWithOdvaException(Exception maybeTimeoutException) {
        return substituteTimeoutException(
                maybeTimeoutException, e -> new OdvaException(ExceptionUtils.extractMessageWithCause(e)));
    }
}
