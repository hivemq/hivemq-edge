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

import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.InMemorySingleWriter;
import com.hivemq.persistence.SingleWriterService;

/**
 * @author Lukas Brandl
 */
public class TestSingleWriterFactory {

    public static SingleWriterService defaultSingleWriter(final @NotNull InternalConfigurationService internalConfigurationService) {
        internalConfigurationService.set(InternalConfigurations.PERSISTENCE_BUCKET_COUNT, "4");
        internalConfigurationService.set(InternalConfigurations.MEMORY_SINGLE_WRITER_THREAD_POOL_SIZE, "1");
        InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.set(100);
        InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC.set(1000);
        InternalConfigurations.SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC.set(100);
        return new InMemorySingleWriter(internalConfigurationService);
    }
}
