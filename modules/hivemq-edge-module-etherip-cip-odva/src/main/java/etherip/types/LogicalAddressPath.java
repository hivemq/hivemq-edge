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
package etherip.types;

import java.util.Objects;

/// A CIP logical address: class, instance and attribute.
///
/// The wire encoding is inherited unchanged from the upstream `CNClassPath`. Its fields are private
/// there, so the three coordinates are kept here as well, for the getters and for equality.
public class LogicalAddressPath extends CNClassPath {

    private final int classCode;
    private final int instance;
    private final int attribute;

    public LogicalAddressPath(final int class_code, final int instance, final int attribute) {
        super(class_code, "Class: " + class_code);
        instance(instance);
        attr(attribute);
        this.classCode = class_code;
        this.instance = instance;
        this.attribute = attribute;
    }

    public int getClass_code() {
        return classCode;
    }

    public int getInstance() {
        return instance;
    }

    public int getAttr() {
        return attribute;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final LogicalAddressPath that)) return false;
        return classCode == that.classCode && instance == that.instance && attribute == that.attribute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classCode, instance, attribute);
    }
}
