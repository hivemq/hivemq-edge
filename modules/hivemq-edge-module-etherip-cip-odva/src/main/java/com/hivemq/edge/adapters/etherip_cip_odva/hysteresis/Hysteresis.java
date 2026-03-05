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
package com.hivemq.edge.adapters.etherip_cip_odva.hysteresis;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class Hysteresis {
    @NotNull
    private final Map<Class<?>, HysteresisChecker<?>> hysteresCheckerMap = new HashMap<>();

    private final ListHysteresisChecker listHysteresisChecker = new ListHysteresisChecker(this);

    public Hysteresis() {
        EqualsHysteresisChecker equalsHysteresisChecker = new EqualsHysteresisChecker();

        hysteresCheckerMap.put(String.class, equalsHysteresisChecker);
        hysteresCheckerMap.put(Boolean.class, equalsHysteresisChecker);
        hysteresCheckerMap.put(Byte.class, new ByteHysteresisChecker());
        hysteresCheckerMap.put(Short.class, new ShortHysteresisChecker());
        hysteresCheckerMap.put(Integer.class, new IntegerHysteresisChecker());
        hysteresCheckerMap.put(Long.class, new LongHysteresisChecker());
        hysteresCheckerMap.put(Float.class, new FloatHysteresisChecker());
        hysteresCheckerMap.put(Double.class, new DoubleHysteresisChecker());
    }

    public <T> boolean isModified(T newValue, T currentValue, @NotNull CipTagDefinition tagDefinition) {
        return isModified(newValue, currentValue, tagDefinition.getHysteresis());
    }

    public <T> boolean isModified(T newValue, T currentValue, @NotNull Double hysteresisValue) {
        if (newValue == null) {
            return currentValue != null;
        } else if (currentValue == null) {
            return true;
        }

        if (newValue instanceof List) {
            return listHysteresisChecker.isModified((List<?>) newValue, (List<?>) currentValue, hysteresisValue);
        } else {
            return isModifiedNonCollection(newValue, currentValue, hysteresisValue);
        }
    }

    private <T> boolean isModifiedNonCollection(
            @NotNull final T newValue, @NotNull final T currentValue, @NotNull Double hysteresisValue) {
        Class<T> valueClass = (Class<T>) newValue.getClass();
        HysteresisChecker<T> hysteresisChecker = (HysteresisChecker<T>) hysteresCheckerMap.get(valueClass);

        if (hysteresisChecker == null) {
            throw new UnsupportedOperationException("Hysteresis for " + valueClass + " is not supported");
        }

        return hysteresisChecker.isModified(newValue, currentValue, hysteresisValue);
    }
}
