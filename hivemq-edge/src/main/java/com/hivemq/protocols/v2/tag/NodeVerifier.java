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

import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The narrow seam through which the {@link SharedNodeVerification} re-issues verification for a node — backed by
 * the protocol adapter's {@code verifyBatch} in production. It exists so the coordinator depends on a single
 * verb, not the whole adapter, and so tests can observe verification requests directly. Always invoked on the
 * actor's single dispatch thread.
 */
@FunctionalInterface
public interface NodeVerifier {

    /**
     * Verify the given nodes against the connected device.
     *
     * @param nodes the nodes to verify.
     */
    void verify(@NotNull List<Node> nodes);
}
