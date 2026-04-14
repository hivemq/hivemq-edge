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
package com.hivemq.edge.adapters.browse.model;

/**
 * Conflict-resolution mode for bulk tag imports.
 *
 * <ul>
 *   <li>{@link #CREATE} — File-only tags are created. Identical tags in both are a noop.
 *       Fails if Edge has edge-only tags or if same tag name has different definition.</li>
 *   <li>{@link #DELETE} — Edge-only tags are deleted. Identical tags in both are kept.
 *       Fails if file contains tags not in Edge or if same tag name has different definition.</li>
 *   <li>{@link #OVERWRITE} — Result equals the file exactly. Edge-only deleted, file-only created, differing overwritten.</li>
 *   <li>{@link #MERGE_SAFE} — File-only created, Edge-only kept. Fails if same tag exists with different properties.</li>
 *   <li>{@link #MERGE_OVERWRITE} — File-only created, Edge-only kept. Same tag with different properties is overwritten.</li>
 * </ul>
 */
public enum ImportMode {
    CREATE,
    DELETE,
    OVERWRITE,
    MERGE_SAFE,
    MERGE_OVERWRITE
}
