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
