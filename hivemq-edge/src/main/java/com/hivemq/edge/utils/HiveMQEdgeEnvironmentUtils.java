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
package com.hivemq.edge.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.util.internal.MacAddressUtil;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Simon L Johnson
 */
public class HiveMQEdgeEnvironmentUtils {

    private static volatile UUID sessionToken;
    private static Object lock = new Object();

    public static @NotNull String generateInstallationToken(){
        return HiveMQEdgeEnvironmentUtils.getDefaultMachineId();
    }

    public static @NotNull String getSessionToken(){
        if(sessionToken == null){
            synchronized (lock){
                if(sessionToken == null){
                    sessionToken = UUID.randomUUID();
                }
            }
        }
        return sessionToken.toString();
    }

    public static @NotNull Map<String, String> generateEnvironmentMap() {

        Map<String, String> map = new HashMap<>();
        map.put("vm", System.getProperty("java.vm.version"));
        map.put("os.arch", System.getProperty("os.arch"));
        map.put("os.name", System.getProperty("os.name"));
        map.put("os.version", System.getProperty("os.version"));
        map.put("processor.count", Runtime.getRuntime().availableProcessors() + "");
        map.put("max.memory", Runtime.getRuntime().maxMemory() + "");
        return map;
    }

    public static String getDefaultMachineId(){
        try {
            return MacAddressUtil.formatAddress(
                    MacAddressUtil.defaultMachineId()).toUpperCase();
        } catch(Exception e){
        }
        return "<unknown>";
    }
}
