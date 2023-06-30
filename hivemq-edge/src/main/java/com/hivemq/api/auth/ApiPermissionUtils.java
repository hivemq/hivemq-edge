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
package com.hivemq.api.auth;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class ApiPermissionUtils {

    public static <T extends Annotation> boolean isAnnotationPresent(@NotNull final Class<T> t, @NotNull final Method method) {
        return getAnnotationIfExists(t, method).isPresent();
    }

    public static <T extends Annotation> boolean isAnnotationPresent(@NotNull final Class<T> t, @NotNull final Class<?> clz) {
        return getAnnotationIfExists(t, clz).isPresent();
    }

    public static <T extends Annotation> Optional<T> getAnnotationIfExists(@NotNull final Class<T> t, @NotNull final Class<?> clz) {
        Preconditions.checkNotNull(t);
        Preconditions.checkNotNull(clz);
        try {
            T a = clz.getAnnotation(t);
            if(a != null){
                return Optional.of(a);
            }
            Class<?>[] interfaces = clz.getInterfaces();
            for (Class<?> c : interfaces){
                a = c.getAnnotation(t);
                if(a != null){
                    return Optional.of(a);
                }
            }
            return Optional.empty();
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Read interfaces for Annotation declarations
     */
    public static <T extends Annotation> Optional<T> getAnnotationIfExists(@NotNull final Class<T> t, @NotNull final Method method) {
        Preconditions.checkNotNull(t);
        Preconditions.checkNotNull(method);
        try {
            T a = method.getAnnotation(t);
            if(a != null){
                return Optional.of(a);
            }
            Class<?> searchClass = method.getDeclaringClass();
            Class<?>[] interfaces = searchClass.getInterfaces();
            for (Class c : interfaces){
                try {
                    Method m = c.getMethod(method.getName(), method.getParameterTypes());
                    T annotation = m.getAnnotation(t);
                    if(annotation != null){
                        return Optional.of(annotation);
                    }
                } catch(NoSuchMethodException e){
                }
            }
            return Optional.empty();
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
