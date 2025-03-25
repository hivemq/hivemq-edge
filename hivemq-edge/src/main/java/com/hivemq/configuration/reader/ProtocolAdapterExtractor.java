package com.hivemq.configuration.reader;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ProtocolAdapterExtractor implements ReloadableExtractor<List<@NotNull ProtocolAdapterEntity>, List<@NotNull ProtocolAdapterEntity>> {
    private volatile @NotNull List<ProtocolAdapterEntity> allConfigs =  List.of();

    private final @NotNull Set<String> tagNames = new CopyOnWriteArraySet<>();

    private volatile @Nullable Consumer<List<@NotNull ProtocolAdapterEntity>> consumer = cfg -> log.debug("No consumer registered yet");

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public ProtocolAdapterExtractor(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    public @NotNull List<ProtocolAdapterEntity> getAllConfigs() {
        return allConfigs;
    }

    public @NotNull Optional<ProtocolAdapterEntity> getAdapterByAdapterId(String adapterId) {
        return allConfigs.stream().filter(adapter -> adapter.getAdapterId().equals(adapterId)).findFirst();
    }

    @Override
    public synchronized Configurator.ConfigResult updateConfig(final HiveMQConfigEntity config) {
        final var newConfigs = List.copyOf(config.getProtocolAdapterConfig());
        return updateTagNames(newConfigs)
                .map(duplicates -> Configurator.ConfigResult.ERROR)
                .orElseGet(() -> {
                    allConfigs = newConfigs;
                    notifyConsumer();
                    return Configurator.ConfigResult.SUCCESS;
                });
    }

    public synchronized Configurator.ConfigResult updateAllAdapters(final @NotNull List<ProtocolAdapterEntity> adapterConfigs) {
        final var newConfigs = List.copyOf(adapterConfigs);
        return updateTagNames(newConfigs)
            .map(duplicates -> Configurator.ConfigResult.ERROR)
            .orElseGet(() -> {
                replaceConfigsAndTriggerWrite(newConfigs);
                return Configurator.ConfigResult.SUCCESS;
            });
    }

    private void replaceConfigsAndTriggerWrite(List<@NotNull ProtocolAdapterEntity> newConfigs) {
        allConfigs = newConfigs;
        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    public synchronized boolean addAdapter(final @NotNull ProtocolAdapterEntity protocolAdapterConfig) {
        final var allConfigsTemp = List.copyOf(allConfigs);
        if (allConfigsTemp.stream().anyMatch(cfg -> protocolAdapterConfig.getAdapterId().equals(cfg.getAdapterId()))) {
            throw new IllegalArgumentException("adapter already exists by id '" +
                    protocolAdapterConfig.getProtocolId() +
                    "'");
        }
        return addTagNamesIfNoDuplicates(protocolAdapterConfig.getTags())
                .map(dupes -> {
                    log.error("Found duplicated tag names: {}", dupes);
                    return false;
                })
                .orElseGet(() -> {
                    final var newConfigs = new ImmutableList.Builder<ProtocolAdapterEntity>()
                            .addAll(allConfigsTemp)
                            .add(protocolAdapterConfig)
                            .build();
                    replaceConfigsAndTriggerWrite(newConfigs);
                    return true;
                });
    }

    public synchronized boolean updateAdapter(
            final @NotNull ProtocolAdapterEntity protocolAdapterConfig) {
        final var duplicateTags = new HashSet<String>();
        final var updated = new AtomicBoolean(false);
        final var newConfigs = allConfigs
                        .stream()
                        .map(oldInstance -> {
                            if(oldInstance.getAdapterId().equals(protocolAdapterConfig.getAdapterId())) {
                                return replaceTagNamesIfNoDuplicates(oldInstance.getTags(), protocolAdapterConfig.getTags())
                                        .map(dupes -> {
                                            duplicateTags.addAll(dupes);
                                            return oldInstance;
                                        })
                                        .orElseGet(() -> {
                                            updated.set(true);
                                            return protocolAdapterConfig;
                                        });
                            } else {
                                return oldInstance;
                            }
                        }).toList();
        if(updated.get()) {
            if(!duplicateTags.isEmpty()) {
                log.error("Found duplicated tag names: {}", duplicateTags);
                return false;
            } else {
                replaceConfigsAndTriggerWrite(newConfigs);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteAdapter(final @NotNull String adapterId) {
        final var newConfigs = new ArrayList<>(allConfigs);
        final var deleted = allConfigs
                .stream()
                .filter(config -> config.getAdapterId().equals(adapterId))
                .findFirst()
                .map(found -> {
                    newConfigs.remove(found);
                    tagNames.removeAll(found.getTags().stream().map(TagEntity::getName).toList());
                    return true;
                })
                .orElse(false);

        if(deleted) {
            replaceConfigsAndTriggerWrite(List.copyOf(newConfigs));
            return true;
        }
        return false;
    }

    private void notifyConsumer() {
        final var consumer = this.consumer;
        if(consumer != null) {
            consumer.accept(allConfigs);
        }
    }

    private Optional<Set<String>> updateTagNames(List<ProtocolAdapterEntity> entities) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();
        entities.stream()
                .flatMap(cfg ->
                        cfg.getTags().stream()).forEach(tag -> {
                    if (newTagNames.contains(tag.getName())) {
                        duplicates.add(tag.getName());
                    } else {
                        newTagNames.add(tag.getName());
                    }
                });

        if(!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while updating: {}", duplicates);
            return Optional.of(duplicates);
        }
        tagNames.clear();
        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    private Optional<Set<String>> addTagNamesIfNoDuplicates(List<TagEntity> newTags) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();
        newTags.forEach(tag -> {
            if (tagNames.contains(tag.getName())) {
                duplicates.add(tag.getName());
            } else {
                newTagNames.add(tag.getName());
            }
        });

        if(!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while adding: {}", duplicates);
            return Optional.of(duplicates);
        }
        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    private Optional<Set<String>> replaceTagNamesIfNoDuplicates(List<TagEntity> oldTags, List<TagEntity> newTags) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();

        final var currentTagNames = new HashSet<>(tagNames);

        currentTagNames.removeAll(oldTags.stream().map(TagEntity::getName).toList());

        newTags.forEach(tag -> {
            if (currentTagNames.contains(tag.getName())) {
                duplicates.add(tag.getName());
            } else {
                newTagNames.add(tag.getName());
            }
        });
        if(!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while replacing: {}", duplicates);
            return Optional.of(duplicates);
        }

        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    @Override
    public synchronized void registerConsumer(final Consumer<List<@NotNull ProtocolAdapterEntity>> consumer) {
        this.consumer = consumer;
        notifyConsumer();
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public synchronized void sync(final @NotNull HiveMQConfigEntity config) {
        config.getProtocolAdapterConfig().clear();
        config.getProtocolAdapterConfig().addAll(allConfigs);
    }
}
