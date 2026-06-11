/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.etherip_cip_odva;

import org.slf4j.bridge.SLF4JBridgeHandler;

public class JULtoSLF4JEnabler {

    private JULtoSLF4JEnabler() {}

    public static void enable() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // FIXME: To be removed
        //        EtherNetIP.logger.log(Level.FINE, "TEST MESSAGE!");
        //        EtherNetIP.logger.log(Level.INFO, "TEST MESSAGE INFO!");
        //        EtherNetIP.logger.log(Level.SEVERE, "TEST MESSAGE SEVERE!");

        // EthernetIP library logs at: etherip.EtherNetIP
    }
}
