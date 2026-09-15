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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * A free port for a test to bind. Drawn at random from {@link #LOWEST} to {@link #HIGHEST} and proven free
 * at the moment it is handed out.
 *
 * <pre>
 *     int get()                    // TCP -- what almost everything wants
 *     int get(Protocol protocol)   // TCP, UDP, or BOTH
 * </pre>
 *
 * {@code protocol} says which port spaces must be free. TCP and UDP are independent address spaces: a number
 * free for TCP can already be taken on UDP, and the reverse. Ask for {@link Protocol#BOTH} when the number
 * goes somewhere that might claim either. That is rare -- of ~160 call sites, three pass {@code BOTH}, all
 * for an MQTT-SN listener, which is UDP.
 *
 * <h2>How this is used, and why bind-to-zero is not an option</h2>
 * The usual pattern is not "open a socket here and now". It is: take a number, write it into a configuration
 * file, and let a service bind it during startup -- often seconds later. Other parties need the number too:
 * the test connects a client to it, and the configuration must name it before anything starts.
 * <p>
 * That rules out asking the operating system for a free port by binding to port 0, which is otherwise the
 * safest approach. Binding to 0 gives you a socket, and the number only afterwards, from a socket you are
 * holding. Here the number must exist <i>before</i> anything binds, and must be knowable by someone other
 * than the binder. Roughly 145 of ~150 call sites are shaped this way.
 * <p>
 * So there is unavoidably a gap between proving the number free and something actually binding it --
 * sometimes a few seconds.
 *
 * <h2>That gap is not the problem you think it is</h2>
 * <b>If you believe a bind failure was caused by this class, you are almost certainly mistaken.</b> That
 * claim has been made many times over the years and was wrong every time but two -- and both of those are
 * fixed, below.
 * <p>
 * The burden is on the claim: explain why, in this particular situation, the argument that follows does not
 * hold. Two things must <b>both</b> happen for a collision:
 * <ol>
 *   <li><b>Another acquisition must fall inside our gap.</b> Only a small slice of any run is spent inside
 *       such a gap, so an arbitrary acquisition landing in ours is unlikely.
 *   <li><b>It must draw exactly our number</b>, out of {@value #RANGE} candidates.
 * </ol>
 * Each is unlikely by itself. They are independent, so together they are <i>very</i> unlikely -- which is why
 * ~150 call sites coexist without trouble. The range is also kept below the block the kernel draws from when
 * a program asks for any free port (see {@link #HIGHEST}), which removes the most plausible outside source
 * entirely.
 * <p>
 * A port number and a stack trace do not meet the burden: they show that a collision happened, not that this
 * class caused it. A leftover service, a container holding a published port, or a previous test that never
 * released will all produce exactly that. Nor does "there is a gap, so this could happen" -- that restates
 * the possibility the argument already accounts for.
 *
 * <h2>The one case that did escape the argument</h2>
 * A caller that takes two ports in a row makes the first condition <b>certain</b> rather than unlikely: the
 * second acquisition is not at risk of landing inside the first one's gap, it is inside it by construction.
 * Only the 1-in-{@value #RANGE} remained, and across ~800 embedded instances per build that came to about 2%
 * of builds -- matching what was seen, one affected build in 28. One instance bound the same number for its
 * MQTT listener and its HTTP API a millisecond apart (EDG-956).
 * <p>
 * That is the shape a valid claim has to take: naming which condition stopped being unlikely, and why.
 *
 * <h2>The second case that escaped it: the probe tested the wrong address</h2>
 * A claim did meet the burden a second time (EDG-986), and it did not involve the gap at all. The probe used
 * {@code new ServerSocket(port)}, which binds the <b>wildcard</b> address. Many callers bind {@code 127.0.0.1}
 * instead -- the XML templates under {@code src/test/resources}, the OPC-UA test server, Jersey's
 * {@code localhost} default. <b>On macOS a wildcard bind and a loopback bind on the same port do not
 * conflict.</b> So the probe could report a port free while it was held on loopback, and would have done so at
 * any moment during the hold, not only inside the gap.
 * <p>
 * Observed: an OPC-UA test server held {@code 127.0.0.1:22217} for 26 seconds; ten seconds in, another fork was
 * handed 22217 and its HTTP API failed to bind. Neither of the two conditions above was involved -- the first
 * did not apply, because there was no gap to land in.
 * <p>
 * {@link #isFree} now probes both addresses. Linux enforces the conflict, which is why CI rarely saw this and
 * the wildcard-only probe looked sufficient for years.
 *
 * <h2>How it works now</h2>
 * A number handed out is remembered, and neither it nor any number sharing its bucket is issued again for the
 * next {@value #BLOCK_FOR} requests, which makes the case above impossible. The record is a fixed array
 * rather than a growing set, so a long-running JVM cannot fill it with ports released long ago -- every
 * operation is constant-time, the memory is fixed, and entries expire on their own, so nothing needs
 * resetting between tests. The array is consulted before any socket is opened, so a rejected candidate costs
 * one array read.
 * <p>
 * What remains is genuinely outside reach: another Gradle fork, or another process on the machine, could
 * still take a number in the gap. That needs both conditions to hold, and no observed failure has ever
 * matched it.
 *
 * <h2>There are three copies of this file</h2>
 * <b>hivemq-edge-test, hivemq-edge, and hivemq-edge's OPC-UA module each carry one, and they must stay
 * line-for-line identical.</b> They had already drifted once: only one probed UDP, so the single improvement
 * anybody had made reached a third of the call sites.
 * <p>
 * <b>Delete two of them once the repositories are merged into a monorepo</b> -- a shared test-fixtures source
 * set makes the duplication unnecessary, and it is only tolerated now because the copies live in different
 * repositories.
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

    /** The one non-wildcard address tests bind. Every other bind in the suite is the wildcard address. */
    private static final String LOOPBACK = "127.0.0.1";

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

    /**
     * Probes the wildcard address <b>and</b> {@value #LOOPBACK}, because a port free on one can be taken on the
     * other. Callers bind one or the other -- roughly 100 of ~160 call sites end up on the wildcard address
     * (the {@code EmbeddedHiveMQExtension} default config and the entity defaults behind it), and roughly 58 on
     * {@value #LOOPBACK} (the XML templates under {@code src/test/resources}, the OPC-UA test server, and
     * Jersey's {@code localhost} default). Probing only one leaves the other half unprotected.
     * <p>
     * On Linux the two conflict, so the wildcard probe alone happened to cover both. <b>On macOS they do
     * not.</b> A wildcard bind succeeds while {@value #LOOPBACK} is held, and the reverse, so the old probe
     * reported a port free that was in use for the whole of a 26-second hold -- not a race in a narrow window,
     * but a check that answered a different question than the one that mattered (EDG-986).
     */
    private static boolean isFree(final int port, final Protocol protocol) {
        if (protocol == Protocol.TCP || protocol == Protocol.BOTH) {
            if (!isTcpFree(port)) {
                return false;
            }
        }
        if (protocol == Protocol.UDP || protocol == Protocol.BOTH) {
            return isUdpFree(port);
        }
        return true;
    }

    /**
     * The two probes are <b>sequential, never held at once</b>. Where the addresses do conflict -- UDP on every
     * platform, TCP on Linux -- holding the first open makes the second fail by construction, and every port
     * would be rejected. Each probe must open and close before the next begins.
     */
    private static boolean isTcpFree(final int port) {
        try (final ServerSocket ignored = new ServerSocket(port)) {
            // closed here, before the loopback probe
        } catch (final IOException ex) {
            return false;
        }
        try (final ServerSocket ignored = boundTo(LOOPBACK, port)) {
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    private static boolean isUdpFree(final int port) {
        try (final DatagramSocket ignored = new DatagramSocket(port)) {
            // closed here, before the loopback probe
        } catch (final IOException ex) {
            return false;
        }
        try (final DatagramSocket ignored = new DatagramSocket(port, InetAddress.getByName(LOOPBACK))) {
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    /** A TCP socket bound to one address rather than the wildcard, which is what {@code new ServerSocket(port)} does. */
    private static ServerSocket boundTo(final String host, final int port) throws IOException {
        final ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(InetAddress.getByName(host), port));
        return socket;
    }
}
