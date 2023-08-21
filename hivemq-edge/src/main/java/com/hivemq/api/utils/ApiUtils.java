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
package com.hivemq.api.utils;

import com.google.common.base.Preconditions;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.UsernamePasswordRoles;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ApiUtils {

    public static boolean hasDefaultUser(List<UsernamePasswordRoles> users){
        if(!users.isEmpty()){
            return users.stream().filter(user -> (UsernamePasswordRoles.DEFAULT_USERNAME.equals(user.getUserName())
                && UsernamePasswordRoles.DEFAULT_PASSWORD.equals(user.getPassword()))).count() > 0;
        }
        return false;
    }

    public static String getWebContextRoot(final @NotNull ApiConfigurationService apiConfigurationService, final boolean trailingSlash){

        List<ApiListener> listeners = apiConfigurationService.getListeners();
        if(listeners == null || listeners.isEmpty()){
            return null;
        }

        String protocol = null;
        int port = 80;
        String host = null;

        for(ApiListener listener : listeners){
            try {
                host = getHostName(listener.getBindAddress());
            } catch(UnknownHostException e){
                host = listener.getBindAddress();
            }
            port = listener.getPort();
            protocol = listener instanceof HttpsListener ? "https" : "http";
            break;
        }
        if(port == 80 || port == 443){
            return String.format("%s://%s%s", protocol, host, trailingSlash ? "/" : "");
        } else {
            return String.format("%s://%s:%s%s", protocol, host, port + "", trailingSlash ? "/" : "");
        }
    }

    public static String getHostName(final @NotNull String name) throws UnknownHostException {
        Preconditions.checkNotNull(name);
        if("127.0.0.1".equals(name) || "0.0.0.0".equals(name)){
            //with no internet the reverse lookup will block for too long here
            //so assume localhost
            return "localhost";
//            return InetAddress.getLocalHost().getHostName();
        }
        InetAddress host = InetAddress.getByName(name);
        return host.getHostName();
    }
}
