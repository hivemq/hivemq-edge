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
package com.hivemq.edge.adapters.opcua.method;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/// The methods of each `METHOD` tag's object, read once per tag and kept until the session ends.
///
/// ## What it caches, and why that is safe
///
/// A method's NodeId is assigned by the server and is stable for as long as the session is: nothing a client
/// does renumbers it. So reading it once per tag and reusing it is correct, and re-browsing on every call would
/// pay a round trip to learn what has not changed.
///
/// What *can* change it is a new session. OPC 10000-3 §5.2.2 is precise about which part: a server **shall**
/// persist a node's identifier and the namespace URI its index refers to, but **may** change the
/// `namespaceIndex` "with future Sessions and therefore a Client shall not assume the namespaceIndex will not
/// change". A remembered NodeId carries an index, so an entry that outlives its session may name a different
/// node than the one it was read for.
///
/// So the invalidation rule is **one session, one set of entries**, and {@link #invalidate()} is called from
/// both seams that start a session:
///
///   - claiming the connection, which covers a first connect and every reconnect the adapter drives by
///     building a replacement connection;
///   - the session-active hook, which covers a reconnect Milo performs *inside* the existing client. Milo owns
///     the session lifecycle: when the link drops and recovers it activates a fresh session and no connection
///     is swapped, so this seam is the only thing that learns of it. Attaching invalidation solely to the
///     connection swap left those entries in place across exactly the event that can renumber them.
///
/// There is still no expiry to tune, because what makes an entry stale is precisely what starts a new session.
///
/// ## Who fills it
///
/// The adapter pre-warms every method tag once it is connected, one tag at a time, so that a request is
/// normally a hit -- on a device that serves metadata slowly the browse can take minutes, and paying for it in
/// the first request made it time out while a later one succeeded.
///
/// A request still fills what the pre-warm did not reach, because the pre-warm is allowed to fail and is
/// abandoned when a new session begins. That costs nothing: a failure leaves no entry (below), so the request
/// browses exactly as it would have, and reports the failure to the caller that provoked it.
///
/// This used to be filled only on first use, on the stated grounds that a cache filled at connect would be
/// "silently empty on an adapter that is connected and not browse-ready", reporting a method that exists as
/// absent for the whole session. That is not a hazard this cache has: an entry is kept only for a verdict that
/// is durable, and everything else is evicted, so there is no state in which it answers "absent" from a browse
/// that did not happen.
///
/// ## In-flight requests share one browse
///
/// The value is a *future*, not a result. Two requests arriving for one tag before the browse completes therefore
/// wait on the same browse instead of issuing two, which matters because a request is admitted as soon as the
/// previous write completes and a method tag can have several in flight.
///
/// Neither a thrown browse nor an `Unverifiable` outcome is kept, so an unanswered device does not become a
/// permanent answer about it. Nor is a `Verified` whose `complete` flag is false -- a climb up the type
/// hierarchy that could not read a level found real methods but not necessarily all of them. A `Rejected`
/// outcome *is* kept: that one is durable for the session.
public final class MethodCache {

    private final @NotNull Map<String, CompletableFuture<MethodVerifier.Result>> perTag = new ConcurrentHashMap<>();

    private final @NotNull AtomicLong generation = new AtomicLong();

    /// The methods of this tag's object, read if they are not already known.
    ///
    /// @param tagName the tag, which is the cache key -- two tags may name one object and still be configured
    ///                differently, and the per-tag answer is what a request needs.
    /// @param read    performs the browse. Called at most once per tag per session, and not at all on a hit.
    public @NotNull CompletableFuture<MethodVerifier.Result> get(
            final @NotNull String tagName, final @NotNull Supplier<CompletableFuture<MethodVerifier.Result>> read) {

        // A relay future is stored, and the read's outcome is copied into it. Two reasons it is not simply the
        // read's own future:
        //
        //   - The entry to evict has to be the object the map holds. A future derived by whenComplete is a
        //     different object, so removing the original would never match and a failed read would be remembered
        //     for the session.
        //   - The eviction callback has to name that object, which cannot be written before it exists. Relaying
        //     breaks the cycle: the relay is created first, stored, and only then fed.
        final CompletableFuture<MethodVerifier.Result> relay = new CompletableFuture<>();
        final CompletableFuture<MethodVerifier.Result> entry = perTag.computeIfAbsent(tagName, key -> relay);
        if (entry != relay) {
            return entry;
        }

        read.get().whenComplete((result, throwable) -> {
            // An unanswered device must not become a permanent fact about it. Two cases to drop, and one to keep:
            //
            //   throwable    -- the browse threw. Nothing was learned.
            //   Unverifiable -- the device could not be asked. Same: the answer is unknown, and the next request
            //                   should ask again rather than inherit a verdict about a connection that may since
            //                   have recovered.
            //   Rejected     -- KEPT. "This node is not an object" and "this node is not there" are durable for
            //                   the session; re-browsing on every request would learn the same thing and cost a
            //                   round trip to do it.
            if (throwable != null) {
                perTag.remove(tagName, relay);
                relay.completeExceptionally(throwable);
                return;
            }
            if (result instanceof MethodVerifier.Result.Unverifiable
                    || (result instanceof final MethodVerifier.Result.Verified verified && !verified.complete())) {
                // Incomplete joins Unverifiable for the same reason: a level of the type hierarchy the device
                // could not answer about leaves methods undiscovered, and keeping the partial list would
                // report an inherited or static method as absent for the rest of the session. The methods it
                // did find are still returned to this caller -- only the remembering is declined.
                perTag.remove(tagName, relay);
            }
            relay.complete(result);
        });
        return relay;
    }

    /// Forgets everything, because the session that made it true has ended.
    public void invalidate() {
        generation.incrementAndGet();
        perTag.clear();
    }

    /// Whether this tag's methods are already known, without asking the device if they are not.
    ///
    /// {@link #get} cannot answer this: on a miss it browses, which is its job. This exists so a test can check
    /// that the connect-time pre-warm filled the cache rather than inferring it from a later request, which
    /// would fill the cache itself and prove nothing.
    public boolean isKnown(final @NotNull String tagName) {
        final CompletableFuture<MethodVerifier.Result> entry = perTag.get(tagName);
        return entry != null && entry.isDone() && !entry.isCompletedExceptionally();
    }

    /// Which set of entries is current. Changes on every {@link #invalidate()}.
    ///
    /// For a caller that fills the cache over a long stretch rather than in one request -- the connect-time
    /// pre-warm, which browses one tag at a time and may run for minutes on a slow device. Such a run can still
    /// be going when its session ends, and an entry it then stored would belong to the dead session while the
    /// cache had already been cleared for the live one. Reading this before a run and comparing before each tag
    /// is how the run notices.
    ///
    /// Object identity of the connection is not enough to notice: Milo can activate a fresh session *inside* an
    /// existing client, so the connection object is unchanged across exactly the event that renumbers node ids.
    /// That seam is one of the two that calls {@link #invalidate()}, which is why the counter lives here rather
    /// than being inferred from the connection.
    public long generation() {
        return generation.get();
    }
}
