package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProtocolAdapterFactoryManager {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterFactoryManager.class);
    private volatile @NotNull Map<String, ProtocolAdapterFactory<?>> factoryMap;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull EventService eventService;

    public ProtocolAdapterFactoryManager(final @NotNull ModuleLoader moduleLoader, final @NotNull EventService eventService, final boolean writingEnabled) {
        this.moduleLoader = moduleLoader;
        this.eventService = eventService;
        factoryMap = findAllAdapters(moduleLoader, eventService, writingEnabled);
    }

    public @NotNull Optional<ProtocolAdapterFactory<?> > get(final @NotNull String protocolAdapterType) {
        return Optional.ofNullable(factoryMap.get(protocolAdapterType));
    }

    public @NotNull Map<String, ProtocolAdapterInformation> getAllAvailableAdapterTypes() {
        return factoryMap.values()
                .stream()
                .map(ProtocolAdapterFactory::getInformation)
                .collect(Collectors.toMap(ProtocolAdapterInformation::getProtocolId, o -> o));
    }

    public void writingEnabledChanged(final boolean writingEnabled) {
        factoryMap = findAllAdapters(moduleLoader, eventService, writingEnabled);
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, ProtocolAdapterFactory<?>> findAllAdapters(final @NotNull ModuleLoader moduleLoader, final @NotNull EventService eventService, final boolean writingEnabled) {
        Map<String, ProtocolAdapterFactory<?>> factoryMap = new HashMap<>();
        final List<Class<? extends ProtocolAdapterFactory>> implementations =
                moduleLoader.findImplementations(ProtocolAdapterFactory.class);

        implementations.add(SimulationProtocolAdapterFactory.class);

        for (final Class<? extends ProtocolAdapterFactory> factoryClass : implementations) {
            try {
                final ProtocolAdapterFactory<?> protocolAdapterFactory = findConstructorAndInitialize(factoryClass, eventService, writingEnabled);
                if (log.isDebugEnabled()) {
                    log.debug("Discovered protocol adapter implementation {}.", factoryClass.getName());
                }
                final ProtocolAdapterInformation information = protocolAdapterFactory.getInformation();
                factoryMap.put(information.getProtocolId(), protocolAdapterFactory);
            } catch (final InvocationTargetException | InstantiationException | IllegalAccessException |
                           NoSuchMethodException e) {
                log.error("Not able to load module, reason: {}.", e.getMessage());
            }
        }

        log.info("Discovered {} protocol adapter-type(s): [{}].",
                factoryMap.size(),
                factoryMap.values()
                        .stream()
                        .map(protocolAdapterFactory -> "'" +
                                protocolAdapterFactory.getInformation().getProtocolName() +
                                "'")
                        .collect(Collectors.joining(", ")));
        return factoryMap;
    }

    private static ProtocolAdapterFactory<?> findConstructorAndInitialize(final @NotNull Class<? extends ProtocolAdapterFactory> factoryClass, final @NotNull EventService eventService, final boolean writingEnabled)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Constructor<?>[] declaredConstructors = factoryClass.getDeclaredConstructors();
        // check all possible constructors to enable backwards compatibility
        for (final Constructor<?> declaredConstructor : declaredConstructors) {
            final Parameter[] parameters = declaredConstructor.getParameters();
            // likely custom protocol adapter implementations still have the old default no-arg constructor.
            if (parameters.length == 0) {
                return factoryClass.getDeclaredConstructor().newInstance();
            }

            // current format: ProtocolAdapterFactoryInput expandable interface that will be backwards co patible if methods get added.
            if (parameters.length == 1 && parameters[0].getType().equals(ProtocolAdapterFactoryInput.class)) {
                final ProtocolAdapterFactoryInput protocolAdapterFactoryInput =
                        new ProtocolAdapterFactoryInputImpl(writingEnabled, eventService);
                return factoryClass.getDeclaredConstructor(ProtocolAdapterFactoryInput.class)
                        .newInstance(protocolAdapterFactoryInput);
            }

            log.warn("No fitting constructor was found to initialize adapter factory class '{}'.", factoryClass);
        }
        throw new IllegalAccessException();
    }
}
