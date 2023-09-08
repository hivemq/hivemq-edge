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
