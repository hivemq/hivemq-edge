package com.hivemq.bootstrap.services;

import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.RestComponentsService;

public interface CompleteBootstrapService extends PersistenceBootstrapService {

    @NotNull Persistences persistences();

    @NotNull RestComponentsService restComponentsService();

    @NotNull HandlerService handlerService();
}
