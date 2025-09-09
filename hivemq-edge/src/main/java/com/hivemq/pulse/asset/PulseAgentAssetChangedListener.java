/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.asset;

import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.pulse.utils.PulseAgentAssetDiffUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PulseAgentAssetChangedListener implements ExternalAssetProvider.AssetChangedListener {
    private final @NotNull PulseExtractor pulseExtractor;

    public PulseAgentAssetChangedListener(final @NotNull PulseExtractor pulseExtractor) {
        this.pulseExtractor = pulseExtractor;
    }

    public @NotNull PulseExtractor getPulseExtractor() {
        return pulseExtractor;
    }

    @Override
    public void onAssetsChanged(final @NotNull List<Asset> remoteAssets) {
        pulseExtractor.setPulseEntity(PulseAgentAssetDiffUtils.resolve(pulseExtractor.getPulseEntity(), remoteAssets));
    }
}
