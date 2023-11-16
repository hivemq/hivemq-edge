package com.hivemq.extensions.core;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface CoreModuleMain {

    void start(@NotNull CoreModuleService coreModuleService);

}
