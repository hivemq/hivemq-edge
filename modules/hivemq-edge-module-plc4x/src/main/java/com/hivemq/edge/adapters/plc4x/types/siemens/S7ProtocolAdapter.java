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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapter extends AbstractPlc4xAdapter<S7AdapterConfig> {

    static final String
            CONTROLLER_TYPE = "controller-type",   //As part of the connection process, usually the PLC4X S7 driver would try to identify the remote device. However some devices seem to have problems with this and hang up or cause other problems. In such a case, providing the controller-type will skip the identification process and hereby avoid this type of problem.
            LOCAL_RACK = "local-rack",          //Rack value for the client. Defaults to 1. Default value: 1
            LOCAL_SLOT = "local-slot",          //Slot value for the client. Defaults to 1. Default value: 1
            LOCAL_TSAP = "local-tsap",          //tsap . Default value: 0
            REMOTE_RACK = "remote-rack",        //Rack value for the remote main CPU (PLC). Defaults to 0. Default value: 0
            REMOTE_RACK_2 = "remote-rack2",     //Rack value for the remote secondary CPU (PLC). Defaults to 0. Default value: 0
            REMOTE_SLOT = "remote-slot",        //Slot value for the remote main CPU (PLC). Defaults to 0. Default value: 0
            REMOTE_SLOT_2 = "remote-slot2",     //Slot value for the remote secondary CPU (PLC). Defaults to 0. Default value: 0
            REMOTE_TSAP = "remote-tsap",        //Default value: 0
            PDU_SIZE = "pdu-size",              //Maximum size of a data-packet sent to and received from the remote PLC. During the connection process both parties will negotiate a maximum size both parties can work with and is equal or smaller than the given value is used. The driver will automatically split up large requests to not exceed this value in a request or expected response. Default value: 1024 bytes
            MAX_AMQ_CALLER = "max-amq-caller",  //Maximum number of unconfirmed requests the PLC will accept in parallel before discarding with errors. This parameter also will be negotiated during the connection process and the maximum both parties can work with and is equal or smaller than the given value is used. The driver will automatically take care not exceeding this value while processing requests. Too many requests can cause a growing queue. Default value: 8
            MAX_AMQ_CALLEE = "max-amq-callee",  //Maximum number of unconfirmed responses or requests PLC4X will accept in parallel before discarding with errors. This option is available for completeness and is correctly handled out during the connection process, however it is currently not enforced on PLC4X’s side. So if a PLC would send more messages than agreed upon, these would still be processed. Default value: 8
            PING = "ping",                      //If your application requires sampling times greater than the set "read-timeout" time, it is important that the PING option is activated, this will prevent the TCP channel from being closed unnecessarily. Default value: false
            PING_TIME = "ping-time",            //Time value in seconds at which the execution of the PING will be scheduled. Generally set by developer experience, but generally should be the same as (read-timeout / 2). Default value: -1 seconds
            RETRY_TIME = "retry-time",          //Time for supervision of TCP channels. If the channel is not active, a safe stop of the EventLoop must be performed, to ensure that no additional tasks are created. Default value: 4 seconds
            READ_TIMEOUT = "read-timeout";      //This is the maximum waiting time for reading on the TCP channel. As there is no traffic, it must be assumed that the connection with the interlocutor was lost and it must be restarted. When the channel is closed, the "fail over" is carried out in case of having the secondary channel, or it is expected that it will be restored automatically, which is done every 4 seconds. Default value: 8 seconds.


    public S7ProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final S7AdapterConfig adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected String getProtocolHandler() {
        return "s7";
    }

    @Override
    protected AbstractPlc4xAdapter.ReadType getReadType() {
        return ReadType.Read;
    }

    @Override
    protected Map<String, String> createQueryStringParams(final @NotNull S7AdapterConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put(CONTROLLER_TYPE, nullSafe(config.getControllerType()));
        map.put(REMOTE_RACK, nullSafe(config.getRemoteRack()));
        map.put(REMOTE_RACK_2, nullSafe(config.getRemoteRack2()));
        map.put(REMOTE_SLOT, nullSafe(config.getRemoteSlot()));
        map.put(REMOTE_SLOT_2, nullSafe(config.getRemoteSlot2()));
        map.put(REMOTE_TSAP, nullSafe(config.getRemoteTsap()));
        map.put(PDU_SIZE, nullSafe(config.getPduSize()));
        map.put(MAX_AMQ_CALLER, nullSafe(config.getMaxAmqCaller()));
        map.put(MAX_AMQ_CALLEE, nullSafe(config.getMaxAmqCallee()));
        map.put(PING, nullSafe(config.getPing()));
        map.put(PING_TIME, nullSafe(config.getPingTime()));
        map.put(RETRY_TIME, nullSafe(config.getRetryTime()));
        map.put(READ_TIMEOUT, nullSafe(config.getRetryTimeout()));
        return map;
    }
}
