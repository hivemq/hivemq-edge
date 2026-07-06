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
package com.hivemq.edge.adapters.etherip_cip_odva.config;

/**
 * Direction of a tag from the <em>adapter's</em> point of view: whether the adapter reads it northbound
 * (polls/publishes it) and/or writes it southbound. This is about how the adapter uses the tag, not a claim
 * about what the device attribute physically permits — e.g. a {@code WRITE_ONLY} tag's attribute may still be
 * readable on the device, and a {@code PARTIAL_WRITE} to it does read it (read-modify-write).
 * <p>
 * Determines whether a southbound mapping is allowed for the tag, and is part of the tag grouping key so that
 * read-only and write-only tags at the same address do not mix.
 */
public enum CipReadWrite {
    READ_ONLY, // polled/published northbound; no southbound mapping allowed
    WRITE_ONLY, // southbound only (e.g. a command attribute); not polled/published northbound
    READ_WRITE; // both polled northbound and writable southbound

    public boolean isReadable() {
        return this != WRITE_ONLY;
    }

    public boolean isWritable() {
        return this != READ_ONLY;
    }
}
