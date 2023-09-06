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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public interface HttpConstants {

    enum METHOD {
        GET, POST, HEAD, PUT, DELETE, OPTIONS, CONNECT, TRACE, PATCH
    }

    String SLASH = "/";
    String HTTP = "http";
    String HTTPS = "https";
    String PROTOCOL_SEP = "://";
    String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String HTTP_URL_REGEX = "https?:\\/\\/(?:w{1,3}\\.)?[^\\s.]+(?:\\.[a-z]+)*(?::\\d+)?((?:\\/\\w+)|(?:-\\w+))*\\/?(?![^<]*(?:<\\/\\w+>|\\/?>))";
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    String CONTENT_TYPE_HEADER = "Content-Type";
    String USER_AGENT_HEADER = "User-Agent";
    String CONTENT_ENCODING_HEADER = "Content-Encoding";
    String LOCATION_HEADER = "Location";
    String AUTH_HEADER = "Authorization";
    String BASIC_AUTH_CHALLENGE_HEADER = "WWW-Authenticate";
    String BEARER_TOKEN_HEADER = "Bearer %s";
    String BASIC_AUTH_HEADER = "Basic %s";
    String BASIC_AUTH_REALM = "Basic realm=\"%s\"";
    String HTML_MIME_TYPE = "text/html";
    String PLAIN_MIME_TYPE = "text/plain";
    String JSON_MIME_TYPE = "application/json";
    String BASE64_ENCODED_VALUE = "data:%s;base64,%s";
    String DEFAULT_MIME_TYPE = HTML_MIME_TYPE;

    int SC_CONTINUE = 100;
    int SC_SWITCHING_PROTOCOLS = 101;
    int SC_OK = 200;
    int SC_CREATED = 201;
    int SC_ACCEPTED = 202;
    int SC_NON_AUTHORITATIVE_INFORMATION = 203;
    int SC_NO_CONTENT = 204;
    int SC_RESET_CONTENT = 205;
    int SC_PARTIAL_CONTENT = 206;
    int SC_MULTIPLE_CHOICES = 300;
    int SC_MOVED_PERMANENTLY = 301;
    int SC_MOVED_TEMPORARILY = 302;
    int SC_FOUND = 302;
    int SC_SEE_OTHER = 303;
    int SC_NOT_MODIFIED = 304;
    int SC_USE_PROXY = 305;
    int SC_TEMPORARY_REDIRECT = 307;
    int SC_BAD_REQUEST = 400;
    int SC_UNAUTHORIZED = 401;
    int SC_PAYMENT_REQUIRED = 402;
    int SC_FORBIDDEN = 403;
    int SC_NOT_FOUND = 404;
    int SC_METHOD_NOT_ALLOWED = 405;
    int SC_NOT_ACCEPTABLE = 406;
    int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    int SC_REQUEST_TIMEOUT = 408;
    int SC_CONFLICT = 409;
    int SC_GONE = 410;
    int SC_LENGTH_REQUIRED = 411;
    int SC_PRECONDITION_FAILED = 412;
    int SC_REQUEST_ENTITY_TOO_LARGE = 413;
    int SC_REQUEST_URI_TOO_LONG = 414;
    int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    int SC_EXPECTATION_FAILED = 417;
    int SC_INTERNAL_SERVER_ERROR = 500;
    int SC_NOT_IMPLEMENTED = 501;
    int SC_BAD_GATEWAY = 502;
    int SC_SERVICE_UNAVAILABLE = 503;
    int SC_GATEWAY_TIMEOUT = 504;
    int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

    Map<String,String> MIME_MAP = new HashMap(){{
        put("appcache", "text/cache-manifest");
        put("css", "text/css");
        put("woff", "font/woff");
        put("woff2", "font/woff2");
        put("ttf", "font/ttf");
        put("gif", "image/gif");
        put("htm", "text/html");
        put("html", "text/html");
        put("ico", "image/vnd.microsoft.icon");
        put("js", "application/javascript");
        put("json", "application/json");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("mp4", "video/mp4");
        put("pdf", "application/pdf");
        put("png", "image/png");
        put("svg", "image/svg+xml");
        put("xml", "application/xml");
        put("zip", "application/zip");
        put("md", "text/plain");
        put("txt", "text/plain");
        put("webp", "image/webp");
        put("webmanifest", "application/manifest+json");
    }};

}
