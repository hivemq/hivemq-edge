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
package com.hivemq.persistence.retained;

import com.google.common.collect.Sets;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;
import util.TestSingleWriterFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class RetainedMessagePersistenceImplTest {

    private @NotNull AutoCloseable closeableMock;

    private final @NotNull RetainedMessageLocalPersistence localPersistence = mock();
    private final @NotNull PublishPayloadPersistence payloadPersistence = mock();

    private @NotNull RetainedMessagePersistenceImpl retainedMessagePersistence;

    private @NotNull RetainedMessage message;

    private @NotNull SingleWriterService singleWriterService;
    private final @NotNull InternalConfigurationService
            internalConfigurationService = new InternalConfigurationServiceImpl();

    @Before
    public void setUp() throws Exception {
        closeableMock = MockitoAnnotations.openMocks(this);
        internalConfigurationService.set(InternalConfigurations.PERSISTENCE_BUCKET_COUNT, "4");
        message = new RetainedMessage(TestMessageUtil.createMqtt3Publish(), 1000);
        singleWriterService = TestSingleWriterFactory.defaultSingleWriter(internalConfigurationService);
        retainedMessagePersistence =
                new RetainedMessagePersistenceImpl(localPersistence, payloadPersistence,
                        singleWriterService, new Chunker(internalConfigurationService));
    }

    @After
    public void tearDown() throws Exception {
        retainedMessagePersistence.closeDB();
        singleWriterService.stop();
        closeableMock.close();
    }

    @Test(expected = NullPointerException.class)
    public void test_get_topic_null() throws Throwable {
        try {
            retainedMessagePersistence.get(null).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_topic_with_wildcard() throws Throwable {
        try {
            retainedMessagePersistence.get("topic/#").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_topic_with_wildcard_plus() throws Throwable {
        try {
            retainedMessagePersistence.get("topic/+").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_get_with_wildcards_topic_null() throws Throwable {
        try {
            retainedMessagePersistence.getWithWildcards(null).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_with_wildcards_topic_without_wildcard() throws Throwable {
        try {
            retainedMessagePersistence.getWithWildcards("topic").get();
        } catch (final InterruptedException |
                ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void test_get_success_null() throws ExecutionException, InterruptedException {
        when(localPersistence.get("topic", BucketUtils.getBucket("topic", internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT)))).thenReturn(null);
        assertNull(retainedMessagePersistence.get("topic").get());
    }

    @Test
    public void test_get_success_message() throws ExecutionException, InterruptedException {
        when(localPersistence.get("topic", BucketUtils.getBucket("topic", internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT)))).thenReturn(message);
        assertEquals(message, retainedMessagePersistence.get("topic").get());
    }

    @Test
    public void test_get_with_wildcards_success() throws ExecutionException, InterruptedException {
        when(localPersistence.getAllTopics(anyString(), anyInt())).thenReturn(
                Sets.newHashSet("topic/1", "topic/2", "topic/3"));
        final Set<String> topics = retainedMessagePersistence.getWithWildcards("topic/#").get();

        assertTrue(topics.contains("topic/1"));
        assertTrue(topics.contains("topic/2"));
        assertTrue(topics.contains("topic/3"));
    }

    @Test
    public void test_size() {
        retainedMessagePersistence.size();
        verify(localPersistence).size();
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_null() throws Throwable {
        try {
            retainedMessagePersistence.remove(null).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
        verify(localPersistence, never()).remove(anyString(), anyInt());
    }

    @Test
    public void test_remove() throws Throwable {
        try {
            retainedMessagePersistence.remove("topic").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
        verify(localPersistence).remove(eq("topic"), anyInt());
    }

    @Test(expected = NullPointerException.class)
    public void test_persist_topic_null() throws Throwable {
        try {
            retainedMessagePersistence.persist(null, message).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
        verify(localPersistence, never()).put(any(RetainedMessage.class), anyString(), anyInt());
    }

    @Test(expected = NullPointerException.class)
    public void test_persist_message_null() throws Throwable {
        try {
            retainedMessagePersistence.persist("topic", null).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
        verify(localPersistence, never()).put(any(RetainedMessage.class), anyString(), anyInt());
    }

    @Test
    public void test_persist() throws Throwable {
        try {
            retainedMessagePersistence.persist("topic", message).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
        verify(localPersistence).put(eq(message), eq("topic"), anyInt());
        verify(payloadPersistence).add(any(byte[].class), anyLong());
    }

    @Test
    public void test_cleanup() throws ExecutionException, InterruptedException {
        retainedMessagePersistence.cleanUp(1).get();
        verify(localPersistence).cleanUp(1);
    }

    @Test
    public void test_close() throws ExecutionException, InterruptedException {
        retainedMessagePersistence.closeDB().get();
        verify(localPersistence, times(internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT))).closeDB(anyInt());
    }

    @Test
    public void test_clear() throws ExecutionException, InterruptedException {
        retainedMessagePersistence.clear().get();
        verify(localPersistence, times(internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT))).clear(anyInt());
    }
}
