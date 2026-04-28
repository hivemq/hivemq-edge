# HiveMQ Edge SNMP Protocol Adapter

Bridges SNMP-enabled devices (network switches, routers, UPS systems, industrial equipment) to MQTT by polling OIDs at configurable intervals.

Supports SNMPv1, SNMPv2c, and SNMPv3 (USM with authentication and encryption).

Full documentation: [docs/snmp-adapter.adoc](docs/snmp-adapter.adoc)

## Quick Start

### config.xml (SNMPv2c)

```xml
<protocol-adapter>
    <adapterId>network-switch-01</adapterId>
    <protocolId>snmp</protocolId>
    <config>
        <host>192.168.1.100</host>
        <port>161</port>
        <snmpVersion>V2C</snmpVersion>
        <community>public</community>
        <timeoutMillis>3000</timeoutMillis>
        <retries>1</retries>
        <snmpToMqtt>
            <pollingIntervalMillis>5000</pollingIntervalMillis>
            <maxPollingErrorsBeforeRemoval>10</maxPollingErrorsBeforeRemoval>
            <publishChangedDataOnly>false</publishChangedDataOnly>
        </snmpToMqtt>
    </config>
    <tags>
        <tag>
            <name>sys-name</name>
            <description>System name</description>
            <definition>
                <oid>1.3.6.1.2.1.1.5.0</oid>
            </definition>
        </tag>
    </tags>
    <northboundMappings>
        <northboundMapping>
            <topic>network/switch-01/sysname</topic>
            <maxQos>1</maxQos>
            <tagName>sys-name</tagName>
            <messageHandlingOptions>MQTTMessagePerSubscription</messageHandlingOptions>
            <includeTagNames>true</includeTagNames>
            <includeTimestamp>true</includeTimestamp>
        </northboundMapping>
    </northboundMappings>
</protocol-adapter>
```

## Configuration Reference

### Connection

| Property | Default | Description |
|---|---|---|
| `host` | required | IP address or hostname of the SNMP agent |
| `port` | `161` | UDP port |
| `snmpVersion` | required | `V1`, `V2C`, or `V3` |
| `timeoutMillis` | `3000` | Request timeout per retry (ms) |
| `retries` | `1` | Retry attempts on timeout |

### Authentication

| Property | Default | Description |
|---|---|---|
| `community` | `public` | Community string (SNMPv1/v2c) |
| `securityName` | — | USM username (SNMPv3) |
| `authProtocol` | `NONE` | `NONE`, `MD5`, `SHA`, `SHA224`, `SHA256`, `SHA384`, `SHA512` |
| `authPassword` | — | Authentication password (SNMPv3) |
| `privProtocol` | `NONE` | `NONE`, `DES`, `AES128`, `AES192`, `AES256` |
| `privPassword` | — | Privacy passphrase (SNMPv3) |

### SNMP to MQTT

| Property | Default | Description |
|---|---|---|
| `pollingIntervalMillis` | `1000` | Polling interval (ms) |
| `maxPollingErrorsBeforeRemoval` | `10` | Stop after this many consecutive errors |
| `publishChangedDataOnly` | `false` | Only publish when value changes |

### Tags

| Property | Default | Description |
|---|---|---|
| `oid` | required | OID in dotted-decimal notation |
| `dataType` | `AUTO` | `AUTO`, `INTEGER`, `STRING`, `COUNTER32`, `COUNTER64`, `GAUGE`, `TIMETICKS`, `IP_ADDRESS`, `OID`, `OPAQUE` |

## SNMP Type Conversion

| SNMP Type | JSON Type | Notes |
|---|---|---|
| Integer32 | number (int) | |
| Counter32 | number (long) | |
| Counter64 | number (long) | |
| Gauge32 | number (long) | |
| TimeTicks | number (double) | Hundredths-of-second → seconds |
| OctetString | string | Hex if non-printable |
| IpAddress | string | Dotted-decimal |
| OID | string | Dotted-decimal |

## Building

```bash
# Build the shadow JAR
./gradlew :modules:hivemq-edge-module-snmp:shadowJar

# Run tests
./gradlew :modules:hivemq-edge-module-snmp:test
```

The shadow JAR is written to `build/libs/hivemq-edge-module-snmp-<version>-all.jar`.

## License

Copyright 2023-present HiveMQ GmbH — Apache License 2.0

Includes [SNMP4J](https://www.snmp4j.org/) 3.7.8 (Apache 2.0).
