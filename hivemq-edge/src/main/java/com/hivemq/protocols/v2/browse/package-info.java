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
/**
 * The shared browse traversal engine. {@link com.hivemq.protocols.v2.browse.ProtocolAdapterBrowseEngine}
 * is the single implementation of the two-phase DISCOVER → RESOLVE walk over the SDK v2 paginated browse
 * contract: the framework's PAW drives it in production, and the SDK-v2 conformance suites drive the very same
 * class against a real device. It reports through a {@link com.hivemq.protocols.v2.browse.BrowseSink}
 * ({@link com.hivemq.protocols.v2.browse.BrowseOutcome} is the pollable test sink) and yields
 * {@link com.hivemq.protocols.v2.browse.BrowsedNode} results — each a discovered selectable variable with
 * its resolved attributes, assembled path, and deduplicated default tag name.
 */
package com.hivemq.protocols.v2.browse;
