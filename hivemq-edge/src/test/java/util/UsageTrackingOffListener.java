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

import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/// Turns usage tracking off in every test JVM before its first Edge starts, however that Edge is started and
/// whatever its configuration says. An Edge with tracking on sends real phone-home events to the production
/// telemetry endpoint, under the test licences.
///
/// Two flags: the default makes a configuration without `<usage-tracking>` read as disabled, and the forced-off flag
/// keeps the remote service from starting even when a configuration enables tracking explicitly. Both are JVM-wide
/// and the configuration parser reads the default on every load, so they have this one owner and keep one value for
/// the life of the JVM. `hivemq-edge` and `hivemq-edge-test` each carry a copy, registered in their own test
/// services file.
public class UsageTrackingOffListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(final @NotNull TestPlan testPlan) {
        InternalConfigurations.DEFAULT_USAGE_EVENTS_ENABLED.set(false);
        InternalConfigurations.USAGE_TRACKING_FORCED_OFF.set(true);
    }
}
