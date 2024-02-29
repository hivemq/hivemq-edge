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
package com.hivemq.edge.modules.api.adapters;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public interface ProtocolAdapterCapability {

    byte     READ = 1,           /** can the adapter-type read values from the external source and publish them into the system **/
            WRITE = 2,          /** can the adapter-type write values from the local broker publish them into the system **/
            DISCOVER = 4;       /** can the adapter-type discover tags/names from the external source **/


    static boolean supportsCapability(final @NotNull ProtocolAdapterInformation adapterInformation, byte capability){
        Preconditions.checkNotNull(adapterInformation);
        return (adapterInformation.getCapabilities() & capability) == capability;
    }

}
