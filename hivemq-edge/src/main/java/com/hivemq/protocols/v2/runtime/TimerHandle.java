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
package com.hivemq.protocols.v2.runtime;

/**
 * Opaque handle to a timer scheduled on a {@link PriorityTimerQueue}. Pass it back to
 * {@link PriorityTimerQueue#cancel(TimerHandle)} to cancel the timer before it fires.
 * <p>
 * Like the rest of the queue, a handle is touched only on the owning actor's dispatch thread.
 */
public interface TimerHandle {

    /**
     * @return {@code true} while the timer is still pending; {@code false} once it has fired or been cancelled.
     */
    boolean isActive();
}
