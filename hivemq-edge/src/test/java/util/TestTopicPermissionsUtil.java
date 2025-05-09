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

import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("deprecation")
public class TestTopicPermissionsUtil {

    public static TopicPermission getTopicPermission() {
        return new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService()).topicFilter("#").build();
    }


    @SuppressWarnings("NullabilityAnnotations")
    public static class TestTopicPermission implements TopicPermission {

        @NotNull
        @Override
        public String getTopicFilter() {
            return null;
        }

        @NotNull
        @Override
        public PermissionType getType() {
            return null;
        }

        @NotNull
        @Override
        public Qos getQos() {
            return null;
        }

        @NotNull
        @Override
        public MqttActivity getActivity() {
            return null;
        }

        @NotNull
        @Override
        public Retain getPublishRetain() {
            return null;
        }

        @NotNull
        @Override
        public SharedSubscription getSharedSubscription() {
            return null;
        }

        @NotNull
        @Override
        public String getSharedGroup() {
            return null;
        }
    }

}
