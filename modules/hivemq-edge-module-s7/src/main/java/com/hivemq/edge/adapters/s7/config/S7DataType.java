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

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.hivemq.edge.adapters.s7.config.S7Versions.S7_1200;
import static com.hivemq.edge.adapters.s7.config.S7Versions.S7_1500;
import static com.hivemq.edge.adapters.s7.config.S7Versions.S7_200;
import static com.hivemq.edge.adapters.s7.config.S7Versions.S7_300;
import static com.hivemq.edge.adapters.s7.config.S7Versions.S7_400;

/**
 * Documented here:
 * https://support.industry.siemens.com/cs/mdm/109054417?c=69695636619&lc=en-GE
 */
public enum S7DataType {
    BOOL(Boolean.class, 1, List.of(S7_300, S7_400, S7_1200, S7_1500), "Boolean", "https://support.industry.siemens.com/cs/mdm/109054417?c=46422035979&lc=en-GE"),
    BYTE(Byte.class, 8, List.of(S7_300, S7_400, S7_1200, S7_1500), "Byte", "https://support.industry.siemens.com/cs/mdm/109054417?c=56595553163&lc=en-GE"),
    WORD(Byte[].class, 16, List.of(S7_300, S7_400, S7_1200, S7_1500), "Word", "https://support.industry.siemens.com/cs/mdm/109054417?c=56595472523&lc=en-GE"),
    DWORD(Byte[].class, 32, List.of(S7_300, S7_400, S7_1200, S7_1500), "Double Word", "https://support.industry.siemens.com/cs/mdm/109054417?c=56595322763&lc=en-GE"),
    LWORD(Byte[].class, 64, List.of(S7_1500), "Long Word", "https://support.industry.siemens.com/cs/mdm/109054417?c=56595507211&lc=en-GE"),
    USINT(Short.class, 8, List.of(S7_1200, S7_1500), "Unsigned Short Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=46521647883&lc=en-GE"),
    UINT(Integer.class, 16, List.of(S7_1200, S7_1500), "Unsigned Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=46521834123&lc=en-GE"),
    UDINT(Long.class, 32, List.of(S7_1200, S7_1500), "Unsigned Double Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=46521930763&lc=en-GE"),
    ULINT(BigInteger.class, 64, List.of(S7_1500), "Unsigned Long Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=59653945739&lc=en-GE"),
    SINT(Byte.class, 8, List.of(S7_1200, S7_1500), "Short Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=68894861835&lc=en-GE"),
    INT(Short.class, 16, List.of(S7_300, S7_400, S7_1200, S7_1500),"Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=63679745547&lc=en-GE"),
    DINT(Integer.class, 32, List.of(S7_300, S7_400, S7_1200, S7_1500), "Double Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=46521869963&lc=en-GE"),
    LINT(Long.class, 64, List.of(S7_1500), "Long Integer", "https://support.industry.siemens.com/cs/mdm/109054417?c=66825552267&lc=en-GE"),
    REAL(Float.class, 32, List.of(S7_300, S7_400, S7_1200, S7_1500), "Real", "https://support.industry.siemens.com/cs/mdm/109054417?c=68826794251&lc=en-GE"),
    LREAL(Float.class, 64, List.of(S7_1200, S7_1500), "Long Real", "https://support.industry.siemens.com/cs/mdm/109054417?c=68826903691&lc=en-GE"),

    CHAR(Character.class, 8, List.of(S7_300, S7_400, S7_1200, S7_1500), "Character", "https://support.industry.siemens.com/cs/mdm/109054417?c=57152595083&lc=en-GE"),
    WCHAR(Short.class, 16, List.of(S7_1200, S7_1500), "Wide Character", "https://support.industry.siemens.com/cs/mdm/109054417?c=10488733835&lc=en-GE"),
    STRING(String.class, -1, List.of(S7_300, S7_400, S7_1200, S7_1500), "String, 0 to 254 characters only ASCII", "https://support.industry.siemens.com/cs/mdm/109054417?c=63689840011&lc=en-GE"),
    WSTRING(String.class, -1, List.of(S7_1200, S7_1500), "Wide String, 0 to 254 characters only Unicode", "https://support.industry.siemens.com/cs/mdm/109054417?c=61472021771&lc=en-GE"), //

    TIME(Long.class, 32, List.of(S7_300, S7_400, S7_1200, S7_1500), "IEC Time (ms)", "https://support.industry.siemens.com/cs/mdm/109054417?c=61085966091&lc=en-GE"),
    LTIME(BigInteger.class, 64, List.of(S7_1500), "IEC Time (ns)", "https://support.industry.siemens.com/cs/mdm/109054417?c=61410814475&lc=en-GE"),
    //TODO S5TIME https://support.industry.siemens.com/cs/mdm/109054417?c=63689295627&lc=en-GE

    DATE(Short.class, 8, List.of(S7_300, S7_400, S7_1200, S7_1500), "IEC Date, since 01-01-1990 (Year-Month-Day)", "https://support.industry.siemens.com/cs/mdm/109054417?c=46522046859&lc=en-GE"),
    TOD(Long.class, 32, List.of(S7_300, S7_400, S7_1200, S7_1500), "Time Of Day (hours:minutes:seconds.milliseconds)", "https://support.industry.siemens.com/cs/mdm/109054417?c=64869849355&lc=en-GE"),
    LTOD(BigInteger.class, 64, List.of(S7_1500), "Time-of-day (hours:minutes:seconds.nanoseconds)", "https://support.industry.siemens.com/cs/mdm/109054417?c=64869390987&lc=en-GE"),
    DT(BigInteger.class, 64, List.of(S7_1500), "Date and time (year-month-day-hour:minute:second:millisecond)", "https://support.industry.siemens.com/cs/mdm/109054417?c=61473284875&lc=en-GE"),
    LDT(BigInteger.class, 64, List.of(S7_1500), "Date and time (year-month-day-hour:minute:second:nanoseconds)", "https://support.industry.siemens.com/cs/mdm/109054417?c=71834521483&lc=en-GE"),
    DTL(BigInteger.class, 64, List.of(S7_1500), "Date and time (year-month-day-hour:minute:second:nanoseconds)", "https://support.industry.siemens.com/cs/mdm/109054417?c=64682916235&lc=en-GE"),
    ARRAY(Byte[].class, -1, List.of(S7_300, S7_400, S7_1200, S7_1500), "Array of type", "https://support.industry.siemens.com/cs/mdm/109054417?c=52352205963&lc=en-GE");
    //RAW_BYTE_ARRAY TODO: it's not an actual type but is there in the old implementation

    S7DataType(final @NotNull Class<?> javaType, final @NotNull int lengthInBits, final @NotNull List<S7Versions> availableOn, final @NotNull String description, final @NotNull String docs){
        this.javaType = javaType;
        this.lengthInBits = lengthInBits;
        this.availableOn = availableOn;
        this.description = description;
        this.docs = docs;
    }
    private final @NotNull Class<?> javaType;
    private final @NotNull int lengthInBits;
    private final @NotNull List<S7Versions> availableOn;
    private final @NotNull String description;
    private final String docs;

    public Class<?> getJavaType() {
        return javaType;
    }

    public int getLengthInBits() {
        return lengthInBits;
    }

    public List<S7Versions> getAvailableOn() {
        return availableOn;
    }

    public String getDescription() {
        return description;
    }

    public String getDocs() {
        return docs;
    }
}
