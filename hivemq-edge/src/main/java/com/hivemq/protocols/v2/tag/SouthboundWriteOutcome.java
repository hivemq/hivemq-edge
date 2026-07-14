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
package com.hivemq.protocols.v2.tag;

/**
 * The terminal fate of one southbound write, reported back to whoever submitted it (option D's back-pressure
 * signal). Every accepted or rejected write settles exactly once, so a producer that holds the next write until
 * the current one settles can never be left waiting.
 */
public enum SouthboundWriteOutcome {

    /** The device acknowledged the write successfully. */
    SUCCEEDED,

    /** The device acknowledged the write as failed (rejected value, protected register, …). */
    FAILED,

    /**
     * The write was not attempted because one was already in flight — the single-in-flight invariant held. Under
     * option D a well-behaved producer never triggers this; a non-zero count means a producer over-delivered.
     */
    REJECTED_BUSY,

    /** The write was in flight but abandoned before a result (the tag deactivated or the connection was lost). */
    ABORTED
}
