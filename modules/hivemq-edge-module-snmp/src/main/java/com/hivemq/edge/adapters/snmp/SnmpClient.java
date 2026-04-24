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
package com.hivemq.edge.adapters.snmp;

import com.hivemq.edge.adapters.snmp.config.SnmpAuthProtocol;
import com.hivemq.edge.adapters.snmp.config.SnmpPrivProtocol;
import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.SnmpVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 * SNMP4J wrapper that handles SNMP communication for all protocol versions.
 */
@SuppressWarnings("unchecked") // SNMP4J uses raw/wildcard generic types in its API
public class SnmpClient implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SnmpClient.class);

    private final @NotNull SnmpSpecificAdapterConfig config;
    private final @NotNull Snmp snmp;
    private final @NotNull Target<?> target;

    public SnmpClient(final @NotNull SnmpSpecificAdapterConfig config) throws IOException {
        this.config = config;

        // Create UDP transport
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);

        // Configure SNMPv3 if needed
        if (config.getSnmpVersion() == SnmpVersion.V3) {
            configureSnmpV3();
        }

        // Create target
        this.target = createTarget();

        // Start listening
        transport.listen();

        log.debug(
                "SNMP client initialized for {}:{} using version {}",
                config.getHost(),
                config.getPort(),
                config.getSnmpVersion());
    }

    private void configureSnmpV3() {
        // Add default security protocols
        SecurityProtocols.getInstance().addDefaultProtocols();

        // Create USM with local engine ID
        OctetString localEngineID = new OctetString(MPv3.createLocalEngineID());
        USM usm = new USM(SecurityProtocols.getInstance(), localEngineID, 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        // Add user if security name is provided
        if (config.getSecurityName() != null) {
            OctetString securityName = new OctetString(config.getSecurityName());
            OID authProtocol = config.getAuthProtocol().getOid();
            OctetString authPass = config.getAuthPassword() != null ? new OctetString(config.getAuthPassword()) : null;
            OID privProtocol = config.getPrivProtocol().getOid();
            OctetString privPass = config.getPrivPassword() != null ? new OctetString(config.getPrivPassword()) : null;

            UsmUser user = new UsmUser(securityName, authProtocol, authPass, privProtocol, privPass);
            snmp.getUSM().addUser(user);

            log.debug("SNMPv3 user configured: {}", config.getSecurityName());
        }
    }

    private @NotNull Target<?> createTarget() {
        String addressStr = "udp:" + config.getHost() + "/" + config.getPort();
        Address address = GenericAddress.parse(addressStr);

        if (config.getSnmpVersion() == SnmpVersion.V3) {
            UserTarget target = new UserTarget();
            target.setAddress(address);
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(determineSecurityLevel());
            if (config.getSecurityName() != null) {
                target.setSecurityName(new OctetString(config.getSecurityName()));
            }
            target.setTimeout(config.getTimeoutMillis());
            target.setRetries(config.getRetries());
            return target;
        } else {
            CommunityTarget target = new CommunityTarget();
            target.setAddress(address);
            target.setCommunity(new OctetString(config.getCommunity()));
            target.setVersion(
                    config.getSnmpVersion() == SnmpVersion.V1 ? SnmpConstants.version1 : SnmpConstants.version2c);
            target.setTimeout(config.getTimeoutMillis());
            target.setRetries(config.getRetries());
            return target;
        }
    }

    private int determineSecurityLevel() {
        if (config.getAuthProtocol() == SnmpAuthProtocol.NONE) {
            return SecurityLevel.NOAUTH_NOPRIV;
        } else if (config.getPrivProtocol() == SnmpPrivProtocol.NONE) {
            return SecurityLevel.AUTH_NOPRIV;
        } else {
            return SecurityLevel.AUTH_PRIV;
        }
    }

    /**
     * Perform a blocking SNMP GET operation for a single OID.
     * Intended to be called from a virtual thread.
     *
     * @param oidString the OID to read (e.g., "1.3.6.1.2.1.1.5.0")
     * @return the result with value and protocol metadata
     * @throws IOException if the SNMP agent does not respond or returns an error
     */
    public @NotNull SnmpReadResult get(final @NotNull String oidString) throws IOException {
        final PDU pdu = createPDU();
        pdu.add(new VariableBinding(new OID(oidString)));
        pdu.setType(PDU.GET);

        final ResponseEvent<?> response = snmp.send(pdu, target);

        if (response == null || response.getResponse() == null) {
            throw new IOException("SNMP timeout - no response from " + config.getHost() + ":" + config.getPort());
        }

        final PDU responsePDU = response.getResponse();
        if (responsePDU.getErrorStatus() != PDU.noError) {
            throw new IOException("SNMP error: " + responsePDU.getErrorStatusText() + " (index: "
                    + responsePDU.getErrorIndex() + ")");
        }

        if (responsePDU.size() == 0) {
            throw new IOException("SNMP response contained no variable bindings for OID " + oidString);
        }

        final Variable var = responsePDU.get(0).getVariable();
        return new SnmpReadResult(
                convertVariable(var),
                var.getClass().getSimpleName(),
                responsePDU.getErrorStatus(),
                responsePDU.getErrorIndex());
    }

    /**
     * Perform an SNMP WALK operation to retrieve a subtree.
     *
     * @param rootOid the root OID to walk from
     * @return a list of variable bindings in the subtree
     * @throws IOException if the walk fails
     */
    public @NotNull List<VariableBinding> walk(final @NotNull String rootOid) throws IOException {
        List<VariableBinding> results = new ArrayList<>();

        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> events = treeUtils.getSubtree(target, new OID(rootOid));

        for (TreeEvent event : events) {
            if (event.isError()) {
                log.warn("SNMP WALK error at OID {}: {}", rootOid, event.getErrorMessage());
                continue;
            }
            VariableBinding[] vbs = event.getVariableBindings();
            if (vbs != null) {
                results.addAll(Arrays.asList(vbs));
            }
        }

        log.debug("SNMP WALK from {} returned {} results", rootOid, results.size());
        return results;
    }

    /**
     * Test connectivity by reading sysDescr (1.3.6.1.2.1.1.1.0).
     *
     * @return true if the agent responds
     */
    public boolean testConnection() {
        try {
            final SnmpReadResult result = get("1.3.6.1.2.1.1.1.0");
            log.debug("SNMP connection test successful: {}", result.getValue());
            return true;
        } catch (final Exception e) {
            log.debug("SNMP connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private @NotNull PDU createPDU() {
        if (config.getSnmpVersion() == SnmpVersion.V3) {
            return new ScopedPDU();
        }
        return new PDU();
    }

    /**
     * Convert an SNMP Variable to a Java object.
     */
    private @Nullable Object convertVariable(final @NotNull Variable var) {
        if (var instanceof Integer32) {
            return ((Integer32) var).getValue();
        } else if (var instanceof Counter32) {
            return ((Counter32) var).getValue();
        } else if (var instanceof Counter64) {
            return ((Counter64) var).getValue();
        } else if (var instanceof Gauge32) {
            return ((Gauge32) var).getValue();
        } else if (var instanceof TimeTicks) {
            // Convert hundredths of a second to seconds as a double
            return ((TimeTicks) var).getValue() / 100.0;
        } else if (var instanceof IpAddress) {
            return var.toString();
        } else if (var instanceof OID) {
            return var.toString();
        } else if (var instanceof OctetString) {
            OctetString os = (OctetString) var;
            // Try to return as string if it's printable, otherwise as hex
            if (os.isPrintable()) {
                return os.toString();
            } else {
                return os.toHexString();
            }
        } else {
            // For any other type, return string representation
            return var.toString();
        }
    }

    @Override
    public void close() throws IOException {
        if (snmp != null) {
            snmp.close();
            log.debug("SNMP client closed");
        }
    }
}
