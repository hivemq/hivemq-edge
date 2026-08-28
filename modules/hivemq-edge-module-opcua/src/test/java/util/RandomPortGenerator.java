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
 *
 * <h2>Before you blame this class</h2>
 * <b>This does not cause port collisions, and a test that fails to bind is almost never this class's
 * fault.</b> That claim has been made many times over the years and was false every time but one -- and that
 * one exception is fixed, below. Read the following before repeating it.
 * <p>
 * The usual accusation is the window between proving a port free and the caller actually binding it: the
 * socket is closed and the number returned, so in principle someone else could take it in between. In
 * principle. For that to actually happen, <b>two</b> independent things must go wrong:
 * <ol>
 *   <li>another acquisition must fall inside that window, <b>and</b>
 *   <li>it must draw the same number out of {@value #RANGE} candidates.
 * </ol>
 * Both are needed, and both are unlikely, which is why ~150 call sites coexist without trouble. The range also
 * stays clear of the ports the operating system hands out on its own (see {@link #LOWEST} / {@link #HIGHEST}),
 * so the most plausible outside source is excluded by construction.
 * <p>
 * <b>So before claiming this class caused a failure, have real evidence: the port number in the log, and what
 * else held it.</b> If you have that, the two-protection argument above is how to make the case -- name which
 * protection failed and why. "There is a window, so this could happen" is not evidence; it is the shape of an
 * explanation, and it has been wrong nearly every time.
 *
 * <h2>The one exception, and its fix</h2>
 * A caller that takes two ports in a row removes the first protection <i>with certainty</i>: the second draw
 * is not merely likely to land inside the first one's window, it is inside it. Only the 1-in-{@value #RANGE}
 * remained, and across the ~800 embedded instances a CI build starts that became a ~2% chance per build. It
 * was observed once: one instance bound the same port for its MQTT listener and its HTTP API a millisecond
 * apart, and the test failed with "Address already in use" (EDG-956).
 * <p>
 * That is now impossible. A number handed out is remembered, and neither it nor any number sharing its bucket
 * is issued again for the next {@value #BLOCK_FOR} requests. The record is a fixed array rather than a growing
 * set, so a long-running JVM cannot fill it up with ports released long ago; every operation is O(1), the
 * memory is constant, and entries expire on their own, so there is nothing to reset between tests.
 *
 * <h2>What is left</h2>
 * The filter cannot see other processes, so a concurrent Gradle fork or something else on the machine could
 * still take a port between our probe and the caller's bind. That is the original risk with both protections
 * intact, and narrowing the range away from the operating system's own allocations removes its most likely
 * cause. No observed failure has ever matched this shape.
 *
 * <h2>Keeping the copies in step</h2>
 * <b>This file is duplicated in hivemq-edge and hivemq-edge-test and must stay line-for-line identical.</b>
 * The copies had drifted -- one probed UDP as well as TCP and the others did not, so the one improvement
 * anybody made reached a third of the call sites. Keep them in step until a single shared copy is possible.
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

    /**
     * Lowest candidate. Above the well-known and registered ports a test might genuinely meet -- Modbus 502,
     * MQTT 1883, OPC-UA 4840, Postgres 5432, Kafka 9092 and the rest are all below this.
     */
    private static final int LOWEST = 10000;

    /**
     * Highest candidate, chosen to stay <b>below every ephemeral range</b> -- the block the kernel draws from
     * when a program asks for "any free port". Linux starts at 32768 and macOS at 49152, so 32767 is the
     * highest number safe on both.
     * <p>
     * This matters: the previous ceiling of 50000 put 43% of the candidates inside Linux's ephemeral range,
     * where an unrelated outgoing connection could take a port we had just proven free. Narrowing costs 17233
     * candidates and removes the most plausible outside cause of a collision.
     */
    private static final int HIGHEST = 32767;

    /** Number of candidates, {@value #LOWEST} to {@value #HIGHEST} inclusive. */
    private static final int RANGE = HIGHEST - LOWEST + 1;

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
            final int randomNumber = LOWEST + (int) (Math.random() * RANGE);
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
