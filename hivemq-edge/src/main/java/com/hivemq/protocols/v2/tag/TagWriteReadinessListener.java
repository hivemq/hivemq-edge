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

import org.jetbrains.annotations.NotNull;

/**
 * How a tag's <b>write aspect</b> notifies the southbound delivery side that the tag crossed its writability
 * boundary — the seam that lets a {@link com.hivemq.protocols.v2.southbound.SouthboundWriteQueue} reopen its
 * delivery window exactly when the tag can take writes again, instead of guessing from the per-adapter connection
 * state (which fires before the tag has re-verified).
 * <p>
 * The boundary is {@link TagAspectState#isOperating()}: {@link #tagWritable} fires when the aspect <b>enters</b>
 * the operating pair ({@code WAITING_FOR_WRITE_REQUEST}/{@code WAITING_FOR_WRITE_RESULT}) from any pre-operating
 * state — verification succeeded, or the connection was treated as verified; {@link #tagUnwritable} fires when it
 * <b>leaves</b> that pair — deactivation, a lost connection, or a tag-set teardown. Transitions <i>within</i> the
 * operating pair (the normal write round-trip) never fire: a tag mid-write is busy, not unwritable, and closing
 * the delivery window on it would strand the settle-then-deliver cycle.
 * <p>
 * Defined as a small listener so the tag package carries no dependency on the southbound package; the
 * {@link com.hivemq.protocols.v2.southbound.SouthboundWritePlane} implements it, tests supply a recording one.
 * <p>
 * Every method runs on the wrapper's single dispatch thread and must not block it: implementations only flip a
 * queue's delivery window (which at most enqueues to the wrapper mailbox) and return. Because both notifications
 * originate from that one thread, an unwritable→writable pair can never be observed out of order.
 */
public interface TagWriteReadinessListener {

    /**
     * A listener that ignores every notification — the default when no southbound delivery side is attached.
     */
    @NotNull
    TagWriteReadinessListener NONE = new TagWriteReadinessListener() {
        @Override
        public void tagWritable(final @NotNull String tagName) {}

        @Override
        public void tagUnwritable(final @NotNull String tagName) {}
    };

    /**
     * The tag's write aspect entered the operating pair: verified and ready to accept a southbound write.
     *
     * @param tagName the tag whose write aspect became writable.
     */
    void tagWritable(@NotNull String tagName);

    /**
     * The tag's write aspect left the operating pair: deactivated, waiting for the adapter, or torn down. Any write
     * delivered from here on settles {@code ABORTED} until {@link #tagWritable} fires again.
     *
     * @param tagName the tag whose write aspect stopped being writable.
     */
    void tagUnwritable(@NotNull String tagName);
}
