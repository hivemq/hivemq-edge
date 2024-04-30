package com.hivemq.edge;

import com.hivemq.HiveMQEdgeMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ManifestUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VersionProvider {

    private final @NotNull String version;

    @Inject
    public VersionProvider() {
        final String versionFromManifest = ManifestUtils.getValueFromManifest(HiveMQEdgeMain.class, "HiveMQ-Version");
        if(versionFromManifest!=null) {
            version = versionFromManifest;
        } else {
            version = "Development Snapshot";
        }
    }

    public synchronized @NotNull String getVersion() {
        return this.version;
    }


}
