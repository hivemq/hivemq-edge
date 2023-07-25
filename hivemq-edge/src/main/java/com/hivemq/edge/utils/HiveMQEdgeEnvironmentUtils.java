package com.hivemq.edge.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon L Johnson
 */
public class HiveMQEdgeEnvironmentUtils {

    public static @NotNull String generateInstallationToken(){
        return HiveMQEdgeEnvironmentUtils.getLocalhostMacAddress();
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

    public static String getLocalhostMacAddress(){
        try {
            final Enumeration<NetworkInterface> ei = NetworkInterface.getNetworkInterfaces();
            while (ei.hasMoreElements()) {
                final byte[] mac = ei.nextElement().getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++)
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    //-- just return the first hardware address found
                    return sb.toString();
                }
            }
        } catch(Exception e){
        }
        return "<unknown>";
    }
}
