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
package com.hivemq.http.core;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    public static String getBearerTokenAuthenticationHeaderValue(String token) {
        return "Bearer " + token;
    }
    public static String getBasicAuthenticationHeaderValue(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    public static String getMimeTypeFromFileExtension(String fileExtension) throws MimeTypeNotFoundException {
        String mime = HttpConstants.MIME_MAP.get(fileExtension);
        if(mime == null) throw new MimeTypeNotFoundException("unable to find mimeType for " + fileExtension);
        return mime;
    }

    public static String getContextRelativePath(String contextPath, String requestUri){
            return requestUri.substring(requestUri.indexOf(contextPath) + contextPath.length());
    }

    public static String sanitizePath(String resource){
        return resource.replaceAll("//+", "/");
    }

    public static String getFileExtension(String requestUri){
        if(requestUri.contains(".")){
            return requestUri.substring(requestUri.lastIndexOf(".") + 1);
        }
        return null;
    }

    public static String combinePaths(String context, String resource){
        if(context.endsWith("/") && resource.startsWith("/")){
            resource = resource.substring(1);
        }
        if(!context.endsWith("/") && !resource.startsWith("/")){
            resource = "/" + resource;
        }
        return context + resource;
    }

    public static String getContentTypeHeaderValue(String mimeType, String encoding){
        return String.format("%s; charset=%s", mimeType, encoding);
    }

    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> map = new HashMap<>();
        if ((queryString == null) || (queryString.equals(""))) {
            return map;
        }
        String[] params = queryString.split("&");
        for (String param : params) {
            try {
                String[] keyValuePair = param.split("=", 2);
                String name = URLDecoder.decode(keyValuePair[0], "UTF-8");
                if (name == "") {
                    continue;
                }
                String value = keyValuePair.length > 1 ? URLDecoder.decode(
                        keyValuePair[1], "UTF-8") : "";
                map.put(name, value);
            } catch (UnsupportedEncodingException e) {
                // ignore this parameter if it can't be decoded
            }
        }
        return map;
    }

    public static boolean validHttpOrHttpsUrl(@NotNull final String url){
        try {
            new URL(url);
            return true;
        } catch(Exception e){
        }
        return false;
    }


}
