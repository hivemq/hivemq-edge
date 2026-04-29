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
 * <p>
 * Lifecycle: construct → {@link #open()} → use → {@link #close()}.
 * The constructor performs configuration only; no socket is opened until {@code open()} is called.
 */
@SuppressWarnings("unchecked") // SNMP4J uses raw/wildcard generic types in its API
public class SnmpClient implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SnmpClient.class);

    private final @NotNull SnmpSpecificAdapterConfig config;
    private final @NotNull Snmp snmp;
    private final @NotNull Target<?> target;
    private final @NotNull DefaultUdpTransportMapping transport;

    public SnmpClient(final @NotNull SnmpSpecificAdapterConfig config) throws IOException {
        this.config = config;
        this.transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);

        if (config.getSnmpVersion() == SnmpVersion.V3) {
            configureSnmpV3();
        }

        this.target = createTarget();
    }

    /**
     * Opens the UDP socket and starts listening for responses.
     * Must be called once after construction and before any SNMP operations.
     */
    public void open() throws IOException {
        transport.listen();
        log.debug(
                "SNMP client opened for {}:{} using version {}",
                config.getHost(),
                config.getPort(),
                config.getSnmpVersion());
    }

    private void configureSnmpV3() {
        SecurityProtocols.getInstance().addDefaultProtocols();

        final String rawEngineId = config.getEngineId();
        final OctetString localEngineID = rawEngineId != null
                ? OctetString.fromHexString(rawEngineId)
                : new OctetString(MPv3.createLocalEngineID());
        final USM usm = new USM(SecurityProtocols.getInstance(), localEngineID, 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        // securityName is guaranteed non-null for V3 by SnmpSpecificAdapterConfig validation
        final OctetString securityName = new OctetString(config.getSecurityName());
        final OID authProtocol = config.getAuthProtocol().getOid();
        final OctetString authPass =
                config.getAuthPassword() != null ? new OctetString(config.getAuthPassword()) : null;
        final OID privProtocol = config.getPrivProtocol().getOid();
        final OctetString privPass =
                config.getPrivPassword() != null ? new OctetString(config.getPrivPassword()) : null;

        snmp.getUSM().addUser(new UsmUser(securityName, authProtocol, authPass, privProtocol, privPass));
        log.debug("SNMPv3 user configured: {}", config.getSecurityName());
    }

    private @NotNull Target<?> createTarget() {
        final String addressStr = "udp:" + config.getHost() + "/" + config.getPort();
        final Address address = GenericAddress.parse(addressStr);

        if (config.getSnmpVersion() == SnmpVersion.V3) {
            final UserTarget target = new UserTarget();
            target.setAddress(address);
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(determineSecurityLevel());
            target.setSecurityName(new OctetString(config.getSecurityName()));
            target.setTimeout(config.getTimeoutMillis());
            target.setRetries(config.getRetries());
            return target;
        } else {
            final CommunityTarget target = new CommunityTarget();
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
     * Perform a blocking SNMP GET for a single OID.
     * Intended to be called from a virtual thread.
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
        return new SnmpReadResult(convertVariable(var), var.getClass().getSimpleName());
    }

    /**
     * Perform an SNMP WALK to retrieve a subtree.
     */
    public @NotNull List<VariableBinding> walk(final @NotNull String rootOid) throws IOException {
        final List<VariableBinding> results = new ArrayList<>();

        final TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        final List<TreeEvent> events = treeUtils.getSubtree(target, new OID(rootOid));

        for (final TreeEvent event : events) {
            if (event.isError()) {
                log.warn("SNMP WALK error at OID {}: {}", rootOid, event.getErrorMessage());
                continue;
            }
            final VariableBinding[] vbs = event.getVariableBindings();
            if (vbs != null) {
                results.addAll(Arrays.asList(vbs));
            }
        }

        log.debug("SNMP WALK from {} returned {} results", rootOid, results.size());
        return results;
    }

    /**
     * Test connectivity by reading sysDescr (1.3.6.1.2.1.1.1.0).
     */
    public boolean testConnection() {
        try {
            get("1.3.6.1.2.1.1.1.0");
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

    @Nullable
    Object convertVariable(final @NotNull Variable var) {
        if (var instanceof final Integer32 v) {
            return v.getValue();
        } else if (var instanceof final Counter32 v) {
            return v.getValue();
        } else if (var instanceof final Counter64 v) {
            return v.getValue();
        } else if (var instanceof final Gauge32 v) {
            return v.getValue();
        } else if (var instanceof final TimeTicks v) {
            return v.getValue() / 100.0;
        } else if (var instanceof IpAddress || var instanceof OID) {
            return var.toString();
        } else if (var instanceof final OctetString v) {
            return v.isPrintable() ? v.toString() : v.toHexString();
        } else {
            return var.toString();
        }
    }

    @Override
    public void close() throws IOException {
        snmp.close();
        log.debug("SNMP client closed");
    }
}
