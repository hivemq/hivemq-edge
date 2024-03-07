package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import static com.hivemq.configuration.reader.BridgeConfigurator.KEYSTORE_TYPE_JKS;
import static com.hivemq.configuration.reader.BridgeConfigurator.KEYSTORE_TYPE_PKCS12;

public class StoreTypeUtil {

    public static @NotNull String deductType(String path) {
        if (path.endsWith(".p12") || path.endsWith(".pfx")) {
            return KEYSTORE_TYPE_PKCS12;
        } else {
            return KEYSTORE_TYPE_JKS;
        }
    }
}
