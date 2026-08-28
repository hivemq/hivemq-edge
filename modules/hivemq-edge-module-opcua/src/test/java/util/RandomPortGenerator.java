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
package util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * A free port for a test to bind.
 * <p>
 * <b>This file is duplicated in hivemq-edge and hivemq-edge-test and must stay line-for-line identical.</b>
 * The copies had drifted -- one probed UDP as well as TCP and the others did not, so the one improvement
 * anybody made reached a third of the call sites. Keep them in step until a single shared copy is possible.
 *
 * <h2>Two protections, and why the second one exists</h2>
 * A port is proven free by binding it, then the socket is <i>closed</i> and the number returned. The caller
 * binds it later -- often much later, because the number is usually written into a configuration file and
 * bound during startup. That leaves a check-then-use window, which is survivable because a collision needs
 * <b>two</b> independent things to go wrong:
 * <ol>
 *   <li>another acquisition must fall inside that window, <b>and</b>
 *   <li>it must draw the same number out of {@value #RANGE}.
 * </ol>
 * Both are needed, which is why ~150 call sites coexist without colliding.
 * <p>
 * <b>A caller that takes two ports in a row removes the first protection entirely.</b> The second draw is not
 * merely <i>likely</i> to fall inside the first one's window -- it is inside it, with certainty. Only the
 * 1-in-{@value #RANGE} remains, and across the ~800 embedded instances a single CI build starts that becomes
 * a ~2% chance per build. It was observed: one instance bound the same port for its MQTT listener and its
 * HTTP API a millisecond apart, and the test failed with "Address already in use" (EDG-956).
 *
 * <h2>The recently-issued filter</h2>
 * A number handed out is remembered, and neither it nor any number sharing its bucket is issued again for the
 * next {@value #BLOCK_FOR} requests. That restores the first protection <i>within this JVM</i>: two draws in a
 * row can no longer collide.
 * <p>
 * The record is a fixed array rather than a growing set, so a long-running JVM cannot fill it up with ports
 * that were released long ago. Every operation is O(1), the memory is constant, and entries expire on their
 * own -- there is nothing to reset between tests or classes.
 *
 * <h2>What this does not fix</h2>
 * The filter cannot see other processes. A concurrent Gradle fork, or anything else on the machine, can still
 * take a port between our probe and the caller's bind. That is the original residual risk with both
 * protections intact -- a foreign acquisition must land in the window <b>and</b> draw the same number -- and
 * no observed failure has ever matched that shape. Do not read this class as a guarantee.
 *
 * @author Georg Held
 */
public class RandomPortGenerator {

    /**
     * Which port spaces must be free. TCP and UDP are independent: a port free for TCP may already be taken on
     * UDP (a leaked MQTT-SN receiver thread from an earlier test, say), and vice versa. Ask for {@link #BOTH}
     * when the port goes into a configuration file and you cannot tell which listener will claim it.
     */
    public enum Protocol {
        TCP,
        UDP,
        BOTH
    }

    /** Size of the range [10000, 50000]. */
    private static final int RANGE = 40001;

    /** How many subsequent requests a returned number, and its bucket siblings, stay blocked for. */
    private static final int BLOCK_FOR = 32;

    /**
     * One slot per bucket, holding the request counter at which that bucket was last issued. A bucket covers
     * ~39 ports, so blocking one blocks its siblings too -- harmless at this size, and it keeps the lookup a
     * single array index.
     */
    private static final int[] LAST_ISSUED_AT = new int[1024];

    /**
     * Counter of requests. Starts above {@link #BLOCK_FOR} so that the very first check compares against a
     * zero-filled array without treating every bucket as blocked.
     */
    private static int requestCounter = BLOCK_FOR + 1;

    public static int get() {
        return get(Protocol.TCP);
    }

    public static synchronized int get(final Protocol protocol) {
        final int counter = ++requestCounter;
        int tries = 10000;
        while (tries > 0) {
            tries--;
            final int randomNumber = (int) (Math.round(Math.random() * 40000) + 10000);
            final int bucket = randomNumber % LAST_ISSUED_AT.length;
            // Consult the array BEFORE opening any socket: a rejected candidate then costs one array read
            // rather than a socket open and close.
            if (LAST_ISSUED_AT[bucket] + BLOCK_FOR >= counter) {
                continue;
            }
            if (isFree(randomNumber, protocol)) {
                LAST_ISSUED_AT[bucket] = counter;
                return randomNumber;
            }
        }
        throw new RuntimeException("Random port not found");
    }

    private static boolean isFree(final int port, final Protocol protocol) {
        try {
            if (protocol == Protocol.TCP) {
                try (final ServerSocket ignored = new ServerSocket(port)) {
                    return true;
                }
            }
            if (protocol == Protocol.UDP) {
                try (final DatagramSocket ignored = new DatagramSocket(port)) {
                    return true;
                }
            }
            try (final ServerSocket ignoredTcp = new ServerSocket(port);
                    final DatagramSocket ignoredUdp = new DatagramSocket(port)) {
                return true;
            }
        } catch (final IOException ex) {
            return false;
        }
    }
}
