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

public class HttpException extends Exception {

    private String responseMessage;
    private int responseCode;

    private HttpException() {
    }

    public HttpException(int responseCode, String responseMessage){
        this(responseCode, responseMessage, null);
    }

    public HttpException(int responseCode, String responseMessage, Throwable cause){
        super(responseMessage, cause);
        this.responseCode = responseCode;
        setResponseMessage(responseMessage);
    }

    private HttpException(String message) {
        super(message);
    }

    private HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    private HttpException(Throwable cause) {
        super(cause);
    }

    public String getResponseMessage() {
        return responseMessage == null ? super.getMessage() : responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
