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
 * Direction of a tag: whether it can be read (northbound) and/or written (southbound).
 * Determines whether a southbound mapping is allowed for the tag, and is part of the
 * tag grouping key so that read-only and write-only tags at the same address do not mix.
 */
public enum CipReadWrite {
    READ_ONLY, // device attribute is readable only; no southbound mapping allowed
    WRITE_ONLY, // device attribute is writable only (e.g. command attribute); not polled
    READ_WRITE; // device attribute is both readable and writable

    public boolean isReadable() {
        return this != WRITE_ONLY;
    }

    public boolean isWritable() {
        return this != READ_ONLY;
    }
}
