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
package com.hivemq.extensions.services.builder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.TestConfigurationBootstrap;

/**
 * @author Christoph Schäbel
 */
public class TopicPermissionBuilderImplTest {

    private TopicPermissionBuilderImpl topicPermissionBuilder;

    private ConfigurationService configurationService;

    @BeforeEach
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getConfigurationService();
        topicPermissionBuilder = new TopicPermissionBuilderImpl(configurationService);
    }

    @Test
    public void test_topic_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.topicFilter(null));
    }

    @Test
    public void test_topic_empty() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.topicFilter(""));
    }

    @Test
    public void test_topic_invalid() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.topicFilter("#/+"));
    }

    @Test
    public void test_topic_invalid_utf8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.topicFilter("topic" + '\u0000'));
    }

    @Test
    public void test_topic_invalid_utf8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.topicFilter("topic" + '\u0001'));
    }

    @Test
    public void test_topic_valid_utf8_should_not() {
        configurationService.securityConfiguration().setValidateUTF8(false);
        final TopicPermission topicPermission =
                topicPermissionBuilder.topicFilter("topic" + '\u0001').build();
        assertEquals("topic" + '\u0001', topicPermission.getTopicFilter());
    }

    @Test
    public void test_topic_invalid_to_long() {
        configurationService.restrictionsConfiguration().setMaxTopicLength(10);
        assertThatThrownBy(() -> topicPermissionBuilder.topicFilter("topic123456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_shared_topic_invalid() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.topicFilter("$share/g1/#"));
    }

    @Test
    public void test_shared_topic_invalid_utf8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("group" + '\u0000'));
    }

    @Test
    public void test_shared_topic_invalid_utf8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("group" + '\u0001'));
    }

    @Test
    public void test_shared_topic_valid_utf8_should_not() {
        configurationService.securityConfiguration().setValidateUTF8(false);
        final TopicPermission topicPermission = topicPermissionBuilder
                .sharedGroup("group" + '\u0001')
                .topicFilter("topic")
                .build();
        assertEquals("group" + '\u0001', topicPermission.getSharedGroup());
    }

    @Test
    public void test_qos_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.qos(null));
    }

    @Test
    public void test_activity_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.activity(null));
    }

    @Test
    public void test_retain_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.retain(null));
    }

    @Test
    public void test_type_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.type(null));
    }

    @Test
    public void test_shared_sub_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.sharedSubscription(null));
    }

    @Test
    public void test_shared_group_null() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.sharedGroup(null));
    }

    @Test
    public void test_topic_not_set() {

        assertThrows(NullPointerException.class, () -> topicPermissionBuilder.build());
    }

    @Test
    public void test_default_values() {
        final TopicPermission permission =
                topicPermissionBuilder.topicFilter("test/uniqueid/#").build();

        assertEquals("test/uniqueid/#", permission.getTopicFilter());
        assertEquals(TopicPermission.MqttActivity.ALL, permission.getActivity());
        assertEquals(TopicPermission.Retain.ALL, permission.getPublishRetain());
        assertEquals(TopicPermission.Qos.ALL, permission.getQos());
        assertEquals(TopicPermission.PermissionType.ALLOW, permission.getType());
    }

    @Test
    public void test_full_values() {
        final TopicPermission permission = topicPermissionBuilder
                .topicFilter("test/unique2id/#")
                .activity(TopicPermission.MqttActivity.PUBLISH)
                .retain(TopicPermission.Retain.NOT_RETAINED)
                .type(TopicPermission.PermissionType.DENY)
                .qos(TopicPermission.Qos.ONE_TWO)
                .sharedGroup("abc")
                .sharedSubscription(TopicPermission.SharedSubscription.NOT_SHARED)
                .build();

        assertEquals("test/unique2id/#", permission.getTopicFilter());
        assertEquals(TopicPermission.MqttActivity.PUBLISH, permission.getActivity());
        assertEquals(TopicPermission.Retain.NOT_RETAINED, permission.getPublishRetain());
        assertEquals(TopicPermission.Qos.ONE_TWO, permission.getQos());
        assertEquals(TopicPermission.PermissionType.DENY, permission.getType());
        assertEquals(TopicPermission.SharedSubscription.NOT_SHARED, permission.getSharedSubscription());
        assertEquals("abc", permission.getSharedGroup());
    }

    @Test
    public void test_shared_group_invalid_string_wildcard() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("as#"));
    }

    @Test
    public void test_shared_group_invalid_string_plus() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("+"));
    }

    @Test
    public void test_shared_group_empty_string() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup(""));
    }

    @Test
    public void test_shared_group_illegal_character() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("U+0000"));
    }

    @Test
    public void test_shared_group_illegal_character_slash() {

        assertThrows(IllegalArgumentException.class, () -> topicPermissionBuilder.sharedGroup("abc/test"));
    }

    @Test
    public void test_shared_group_root_wildcard_allowed() {
        topicPermissionBuilder.sharedGroup("#");
    }
}
