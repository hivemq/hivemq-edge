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
package com.hivemq.edge.modules.adapters;

/**
 * @author Simon L Johnson
 */
public interface ProtocolAdapterConstants {

    enum TAG {
        UDP,
        TCP,
        SERIAL,
        INTERNET,
        WEB,
        IOT,
        IIOT,
        AUTOMATION,
        FACTORY
    }

    enum CATEGORY {
        CONNECTIVITY("Connectivity","A standard connectivity based protocol, typically web standard.", null),
        INDUSTRIAL("Industrial","Industrial, typically field bus protocols.", null),
        BUILDING_AUTOMATION("Building Automation","Protocols related to building automation",  null),
        SIMULATION("Simulation","Simulation protocols, that emulate real world devices", null);

        CATEGORY(final String displayName, final String description, final String image){
            this.displayName = displayName;
            this.image = image;
            this.description = description;
        }

        final String displayName;
        final String description;
        final String image;

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getImage() {
            return image;
        }
    }

    String ADAPTER_NAME_TOKEN = "adapter.name";
    String ADAPTER_VERSION_TOKEN = "adapter.version";
    String ADAPTER_CLASS_TOKEN = "adapter.class";
    String ADAPTER_PROTOCOL_ID_TOKEN = "adapter.id";
    String ADAPTER_INSTANCE_ID_TOKEN = "adapter.instance.id";

}
