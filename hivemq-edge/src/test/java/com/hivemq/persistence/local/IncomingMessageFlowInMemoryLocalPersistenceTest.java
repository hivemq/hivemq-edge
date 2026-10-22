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
package com.hivemq.persistence.local;

import static com.hivemq.persistence.qos.IncomingMessageFlowPersistence.AddResult.ADDED;
import static com.hivemq.persistence.qos.IncomingMessageFlowPersistence.AddResult.ALREADY_PRESENT;
import static com.hivemq.persistence.qos.IncomingMessageFlowPersistence.AddResult.LIMIT_REACHED;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence.AddResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lukas Brandl
 * @author Dominik Obermaier
 */
public class IncomingMessageFlowInMemoryLocalPersistenceTest {

    private final RestrictionsConfigurationServiceImpl restrictions = new RestrictionsConfigurationServiceImpl();
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private IncomingMessageFlowInMemoryLocalPersistence persistence;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        persistence = new IncomingMessageFlowInMemoryLocalPersistence(restrictions, metricRegistry);
    }

    @AfterEach
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_contains_no_entry_available() {
        assertThat(held("not_available", 1)).isFalse();
    }

    @Test
    public void test_add_new_identifier() {
        assertThat(persistence.add("client", 1)).isEqualTo(ADDED);
        assertThat(held("client", 1)).isTrue();
        assertThat(persistence.size()).isEqualTo(1);
    }

    @Test
    public void test_add_same_identifier_twice_is_a_retransmission() {
        persistence.add("client", 1);

        assertThat(persistence.add("client", 1)).isEqualTo(ALREADY_PRESENT);
        assertThat(persistence.size()).isEqualTo(1);
    }

    @Test
    public void test_remove_entry() {
        persistence.add("client", 1);
        persistence.remove("client", 1);

        assertThat(held("client", 1)).isFalse();
        assertThat(persistence.size()).isZero();
        // a released identifier is new again
        assertThat(persistence.add("client", 1)).isEqualTo(ADDED);
    }

    @Test
    public void test_remove_nonexistent_entry() {
        persistence.remove("nonexistent", 1);

        assertThat(persistence.size()).isZero();
    }

    @Test
    public void test_remove_entry_for_same_client_but_different_message_id() {
        persistence.add("client", 1);
        persistence.remove("client", 2);

        assertThat(held("client", 2)).isFalse();
        assertThat(held("client", 1)).isTrue();
        assertThat(persistence.size()).isEqualTo(1);
    }

    @Test
    public void test_remove_entry_same_message_id_but_different_client() {
        persistence.add("client", 1);
        persistence.remove("client2", 1);

        assertThat(held("client", 1)).isTrue();
    }

    @Test
    public void test_delete() {
        persistence.add("client", 1);
        persistence.add("client", 2);
        persistence.add("client2", 1);

        persistence.delete("client");

        assertThat(held("client", 1)).isFalse();
        assertThat(held("client", 2)).isFalse();
        assertThat(held("client2", 1)).isTrue();
        assertThat(persistence.size()).isEqualTo(1);
    }

    @Test
    public void test_limit_refuses_new_identifiers_but_not_retransmissions() {
        restrictions.setMaxIncomingQos2Flows(2);
        persistence.add("client", 1);
        persistence.add("client2", 1);

        assertThat(persistence.add("client3", 1)).isEqualTo(LIMIT_REACHED);
        assertThat(persistence.add("client", 2)).isEqualTo(LIMIT_REACHED);
        assertThat(persistence.add("client", 1)).isEqualTo(ALREADY_PRESENT);
        assertThat(held("client3", 1)).isFalse();
        assertThat(persistence.size()).isEqualTo(2);
    }

    @Test
    public void test_limit_frees_on_remove_and_delete() {
        restrictions.setMaxIncomingQos2Flows(2);
        persistence.add("client", 1);
        persistence.add("client", 2);

        persistence.remove("client", 1);
        assertThat(persistence.add("client2", 1)).isEqualTo(ADDED);

        persistence.delete("client");
        assertThat(persistence.add("client3", 1)).isEqualTo(ADDED);
        assertThat(persistence.size()).isEqualTo(2);
    }

    @Test
    public void test_client_limit_refuses_the_clients_new_identifiers_only() {
        restrictions.setMaxIncomingQos2FlowsPerClient(2);
        persistence.add("client", 1);
        persistence.add("client", 2);

        assertThat(persistence.add("client", 3)).isEqualTo(LIMIT_REACHED);
        assertThat(persistence.add("client", 1)).isEqualTo(ALREADY_PRESENT);
        assertThat(persistence.add("client2", 1)).isEqualTo(ADDED);
        assertThat(held("client", 3)).isFalse();
        // a refusal for the client's limit takes nothing from the node's
        assertThat(persistence.size()).isEqualTo(3);

        persistence.remove("client", 1);
        assertThat(persistence.add("client", 3)).isEqualTo(ADDED);
    }

    @Test
    public void test_client_limit_is_checked_before_the_node_limit() {
        restrictions.setMaxIncomingQos2FlowsPerClient(1);
        restrictions.setMaxIncomingQos2Flows(1);
        persistence.add("client", 1);

        assertThat(persistence.add("client", 2)).isEqualTo(LIMIT_REACHED);
        assertThat(persistence.add("client2", 1)).isEqualTo(LIMIT_REACHED);
    }

    @Test
    public void test_gauge_reports_size() {
        persistence.add("client", 1);
        persistence.add("client", 2);

        final Object value = metricRegistry
                .getGauges()
                .get(HiveMQMetrics.INCOMING_QOS2_FLOWS_CURRENT.name())
                .getValue();
        assertThat(value).isEqualTo(2L);
    }

    @Test
    public void test_concurrent_adds_pass_the_limit_by_at_most_one_per_thread() throws Exception {
        final int limit = 1_000;
        final int threads = 8;
        final int idsPerThread = 500;
        restrictions.setMaxIncomingQos2Flows(limit);
        executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Integer>> added = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final String client = "client-" + t;
            added.add(executor.submit(() -> {
                start.await();
                int count = 0;
                for (int id = 1; id <= idsPerThread; id++) {
                    if (persistence.add(client, id) == AddResult.ADDED) {
                        count++;
                    }
                }
                return count;
            }));
        }
        start.countDown();
        int total = 0;
        for (final Future<Integer> future : added) {
            total += future.get();
        }

        assertThat(total).isBetween(limit, limit + threads);
        assertThat(persistence.size()).isEqualTo(total);
    }

    @Test
    public void test_concurrent_add_and_delete_keep_the_size_exact() throws Exception {
        executor = Executors.newFixedThreadPool(2);
        final CountDownLatch start = new CountDownLatch(1);
        final Future<?> adder = executor.submit(() -> {
            start.await();
            for (int round = 0; round < 2_000; round++) {
                for (int id = 1; id <= 5; id++) {
                    persistence.add("client", id);
                }
            }
            return null;
        });
        final Future<?> deleter = executor.submit(() -> {
            start.await();
            for (int round = 0; round < 2_000; round++) {
                persistence.delete("client");
            }
            return null;
        });
        start.countDown();
        adder.get();
        deleter.get();

        int held = 0;
        for (int id = 1; id <= 5; id++) {
            if (held("client", id)) {
                held++;
            }
        }
        assertThat(persistence.size()).isEqualTo(held);
    }

    /**
     * Whether the identifier is held, probed through the store's own API: {@code add} answers a held identifier with
     * {@code ALREADY_PRESENT}, and a probe that added it removes it again.
     */
    private boolean held(final String client, final int messageId) {
        final AddResult result = persistence.add(client, messageId);
        if (result == AddResult.ADDED) {
            persistence.remove(client, messageId);
        }
        return result == AddResult.ALREADY_PRESENT;
    }
}
