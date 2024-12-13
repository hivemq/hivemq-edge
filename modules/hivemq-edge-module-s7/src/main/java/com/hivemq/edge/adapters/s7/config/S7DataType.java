/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.s7.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author Simon L Johnson
 */
public enum S7DataType {
    BOOL(Boolean.class),
    BYTE(Byte.class),
    INT16(Short.class),
    UINT16(Short.class),
    INT32(Integer.class),
    UINT32(Integer.class),
    INT64(Long.class),
    REAL(Float.class),
    LREAL(Double.class),
    STRING(String.class),
    DATE(LocalDate.class),
    TIME_OF_DAY(LocalTime.class),
    DATE_AND_TIME(LocalDateTime.class),
    TIME(Long.class);

    S7DataType(Class<?> javaType){
        this.javaType = javaType;
    }
    private Class<?> javaType;

    public Class<?> getJavaType() {
        return javaType;
    }

}
