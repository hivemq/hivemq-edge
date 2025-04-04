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
package com.hivemq.http;

public class HttpStatus {

    public static final int OK_200 = 200;
    public static final int CREATED_201 = 201;
    public static final int NO_CONTENT_204 = 204;

    public static final int BAD_REQUEST_400 = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN_403 = 403;
    public static final int NOT_FOUND_404 = 404;
    public static final int METHOD_NOT_ALLOWED_405 = 405;
    public static final int CONFLICT_409 = 409;

    public static final int GONE_410 = 410;
    public static final int PRECONDITION_FAILED_412 = 412;
    public static final int UNSUPPORTED_MEDIA_TYPE_415 = 415;


    public static final int INTERNAL_SERVER_ERROR_500 = 500;
    public static final int SERVICE_UNAVAILABLE_503 = 503;
    public static final int INSUFFICIENT_STORAGE_507 = 507;

}
