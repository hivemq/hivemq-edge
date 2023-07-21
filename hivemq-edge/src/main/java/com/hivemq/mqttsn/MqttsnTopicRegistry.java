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
package com.hivemq.mqttsn;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initial registry to read through to obtain the correct topicNames for the supplied topicAliasTypes.
 * @author Simon L Johnson
 */
@Singleton
public class MqttsnTopicRegistry
        implements IMqttsnTopicRegistry {
    private static final Logger log =
            LoggerFactory.getLogger(MqttsnTopicRegistry.class);

    private final @NotNull MqttsnConfigurationService configurationService;

    private final Map<String, Map<Integer, MqttsnTopicAlias>> sessionRegistrations
            = new ConcurrentHashMap<>();

    @Inject
    public MqttsnTopicRegistry(final @NotNull MqttsnConfigurationService configurationService){
        this.configurationService = configurationService;
    }

    public String readTopicName(@NotNull String clientId, int topicIdType, byte[] topicData, boolean readNormalAsFullTopic) throws MqttsnProtocolException {

        Preconditions.checkNotNull(clientId);

        String topicName = null;
        if(topicIdType == MqttsnConstants.TOPIC_SHORT){
            if(topicData.length != 2){
                log.warn("Invalid size of topic data for short topic");
                throw new MqttsnProtocolException("invalid size of topic data for short topic");
            }
            if(topicData[1] == 0x00){
                //short topic used and single encoded from codec
                topicData = new byte[]{topicData[0]};
            }
            topicName = new String(topicData, StandardCharsets.UTF_8);
        }
        else if(topicIdType == MqttsnConstants.TOPIC_PREDEFINED){
            int alias = MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
            MqttsnTopicAlias predefinedTopicAlias =
                    configurationService.getPredefinedTopicAliases().get(alias);
            if(predefinedTopicAlias == null){
                log.warn("Unable to obtain predefined topic alias with value {}", alias);
                throw new MqttsnProtocolException("unable to obtain predefined topic alias");
            }
            topicName = predefinedTopicAlias.getTopicName();
        }
        else if(topicIdType == MqttsnConstants.TOPIC_NORMAL){
            if(readNormalAsFullTopic){
                topicName = new String(topicData, StandardCharsets.UTF_8);
                if(log.isTraceEnabled()){
                    log.trace("Considered topicData as String data for normal message (SUBSCRIBE) clientId {} -> {}", clientId, topicName);
                }
            } else {
                int alias = MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
                if(log.isTraceEnabled()){
                    log.trace("Looking up normal topic alias for clientId {} -> {}", clientId, alias);
                }
                Map<Integer, MqttsnTopicAlias> normalAliases = sessionRegistrations.get(clientId);
                if(normalAliases != null){
                    MqttsnTopicAlias registeredAlias = normalAliases.get(alias);
                    if(registeredAlias != null){
                        if(log.isTraceEnabled()){
                            log.trace("Found matching normal topic alias for clientId {} -> {}", clientId, registeredAlias);
                        }
                        topicName = registeredAlias.getTopicName();
                    }
                }
            }
        }

        if(topicName == null){
            log.warn("unable to determine topic from supplied alias configuration; clientId={}, topicTypeId={}, topicData={}",
                    clientId, topicIdType, MqttsnWireUtils.toHex(topicData));
            throw new MqttsnProtocolException("unable to determine topic from supplied alias configuration");
        }

        return topicName;
    }

    public Optional<MqttsnTopicAlias> readTopicAlias(@NotNull String clientId, @NotNull String topicName){

        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(topicName);

        if(topicName.length() <= 2){
            return Optional.of(new MqttsnTopicAlias(topicName, MqttsnTopicAlias.TYPE.SHORT));
        }

        if(sessionRegistrations.containsKey(clientId)){
            Optional<MqttsnTopicAlias> alias = readRegisteredTopic(clientId, topicName);
            if(alias.isPresent()){
                return alias;
            }
        }
        if(!configurationService.getPredefinedTopicAliases().isEmpty()){
            Optional<MqttsnTopicAlias> alias =
                    configurationService.getPredefinedTopicAliases().values().
                            stream().filter(t -> topicName.equals(t.getTopicName())).findFirst();
            if(alias.isPresent()){
                return alias;
            }
        }
        return Optional.empty();
    }

    public int register(@NotNull String clientId, @NotNull String topicName) throws MqttsnProtocolException {

        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(topicName);

        Map<Integer, MqttsnTopicAlias> sessionAlias =
                sessionRegistrations.get(clientId);
        if(sessionAlias == null){
            sessionAlias =
                    sessionRegistrations.get(clientId);
            if(sessionAlias == null){
                sessionAlias = new ConcurrentHashMap<>();
                sessionRegistrations.put(clientId, sessionAlias);
            }
        }

        int aliasId;
        //check to see if its already registered
        //shouldnt need synchronizing since this is always 1 message at a time BUT, just belt and braces
        synchronized (sessionAlias) {
            Optional<MqttsnTopicAlias> alias = readRegisteredTopic(clientId, topicName);
            if (alias.isPresent()) {
                aliasId = alias.get().getAlias();
                if(log.isTraceEnabled()){
                    log.trace("topic alias already existed for clientId {} ({} -> {})", clientId, topicName, aliasId);
                }
            } else {
                aliasId = getNextAvailableUint16(sessionAlias.keySet(), 1);
                sessionAlias.put(aliasId, new MqttsnTopicAlias(topicName, aliasId, MqttsnTopicAlias.TYPE.NORMAL));
                if(log.isTraceEnabled()){
                    log.trace("registered new topic alias for clientId {} ({} -> {})", clientId, topicName, aliasId);
                }
            }
        }

        return aliasId;
    }

    protected Optional<MqttsnTopicAlias> readRegisteredTopic(@NotNull final String clientId, @NotNull final String topicName){
        Map<Integer, MqttsnTopicAlias> m = sessionRegistrations.get(clientId);
        Optional<MqttsnTopicAlias> alias =
                m.values().stream().filter(t -> topicName.equals(t.getTopicName())).findFirst();
        return alias;
    }

    /**
     * This is an expensive operation, so only use when really needed. Will return the next uint16
     * bounded by the start at to uint16 max - disregarding those already used.
     * @param used - existing mappings
     * @param startAt - where the alias should start from
     * @return the next available uint16
     */
    public static int getNextAvailableUint16(@NotNull final Collection<Integer> used, final int startAt) throws MqttsnProtocolException {
        if(used.isEmpty()){
            return startAt;
        }
        if(used.size() == ((0xFFFF - startAt) + 1)) {
            throw new MqttsnProtocolException("all leases taken");
        }
        TreeSet<Integer> sortedIds = new TreeSet<>(used);
        Integer highest = sortedIds.last();
        if(highest >= 0xFFFF){
            throw new MqttsnProtocolException("no alias left for use for client");
        }
        int nextValue = highest.intValue();
        do {
            nextValue++;
            if(!used.contains(nextValue)) return nextValue;
        } while(nextValue <= 0xFFFF);
        throw new MqttsnProtocolException("unable to assign alias");
    }

    @Override
    public void clearSessionAliases(final String clientId) {
        sessionRegistrations.remove(clientId);
    }
}
