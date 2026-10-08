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
/// ## Reading is lazy
///
/// The first call against a tag populates it. Filling it at connect time would tie it to the browse-metadata
/// warm-up, which is deliberately allowed to fail without failing the connection -- so an adapter that is
/// connected and not browse-ready would hold an empty cache and report every method as absent. Reading on first
/// use means the failure is reported to the request that provoked it, and the next request tries again.
///
/// ## In-flight requests share one browse
///
/// The value is a *future*, not a result. Two requests arriving for one tag before the browse completes therefore
/// wait on the same browse instead of issuing two, which matters because a request is admitted as soon as the
/// previous write completes and a method tag can have several in flight.
///
/// Neither a thrown browse nor an `Unverifiable` outcome is kept, so an unanswered device does not become a
/// permanent answer about it. A `Rejected` outcome *is* kept: that one is durable for the session.
public final class MethodCache {

    private final @NotNull Map<String, CompletableFuture<MethodVerifier.Result>> perTag = new ConcurrentHashMap<>();

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
            if (result instanceof MethodVerifier.Result.Unverifiable) {
                perTag.remove(tagName, relay);
            }
            relay.complete(result);
        });
        return relay;
    }

    /// Forgets everything, because the session that made it true has ended.
    public void invalidate() {
        perTag.clear();
    }
}
