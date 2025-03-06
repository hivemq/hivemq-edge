# MTConnect Protocol Adapter

## Design

### Schema Validation

The [Schema](https://github.com/mtconnect/schema) validation is XSD based. That implies a considerable performance overhead if the validation is performed per message. In general, there are 2 options:

1. No Validation - Turn off the schema validation.
2. Complete Validation - Implement the validation in Java to improve the performance.

|                  | No Validation | Complete Validation |
| ---------------- | ------------- | ------------------- |
| Performance      | Excellent     | Slow                |
| Dev Cost         | Low           | High                |
| Malformed Schema | Allow         | Disallow            |
| Custom Schema    | Support       | Not Support         |

### Schema Validation Code Generation

- Download [jaxb-ri](https://eclipse-ee4j.github.io/jaxb-ri/) and unzip it to `modules/hivemq-edge-module-mtconnect` folder.
- Download [Xerces2 Java Binary (XML Schema 1.1)](https://xerces.apache.org/mirrors.cgi) and unzip it to `modules/hivemq-edge-module-mtconnect` folder.
- Add `xerces-2_12_2-xml-schema-1.1/xml-apis.jar` and `xerces-2_12_2-xml-schema-1.1/xercesImpl.jar` to the classpath.
- `git clone https://github.com/mtconnect/schema.git` to `../../../../mtconnect`. Here is a sample directory structure.

```
repos
├── HiveMQ
│   └── hivemq-edge
│       └── modules
│           └── hivemq-edge-module-mtconnect
└── mtconnect
```

- Navigate to `modules/hivemq-edge-module-mtconnect` folder.
- For each of the XML schema files, run the following commands.

```bash
jaxb-ri/bin/xjc.sh -classpath "${CLASSPATH}:xerces-2_12_2-xml-schema-1.1/xml-apis.jar:xerces-2_12_2-xml-schema-1.1/xercesImpl.jar" -d src/main/java -p com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_3 ../../../../mtconnect/schema/MTConnectAssets_1.3.xsd
jaxb-ri/bin/xjc.sh -classpath "${CLASSPATH}:xerces-2_12_2-xml-schema-1.1/xml-apis.jar:xerces-2_12_2-xml-schema-1.1/xercesImpl.jar" -d src/main/java -p com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3 ../../../../mtconnect/schema/MTConnectDevices_1.3.xsd
jaxb-ri/bin/xjc.sh -classpath "${CLASSPATH}:xerces-2_12_2-xml-schema-1.1/xml-apis.jar:xerces-2_12_2-xml-schema-1.1/xercesImpl.jar" -d src/main/java -p com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_3 ../../../../mtconnect/schema/MTConnectError_1.3.xsd
jaxb-ri/bin/xjc.sh -classpath "${CLASSPATH}:xerces-2_12_2-xml-schema-1.1/xml-apis.jar:xerces-2_12_2-xml-schema-1.1/xercesImpl.jar" -d src/main/java -p com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3 ../../../../mtconnect/schema/MTConnectStreams_1.3.xsd
```

- After all the schema files are generated, run test case `MtConnectSchemaJsonAnnotationTest` to convert the XML annotations to Jackson annotations. This is a one-time process and the test case is able to generate the Jackson annotations in an incremental way. By default, this test case is skipped if jaxb-ri or xerces is not found.

## Test

### Test Bed

There is a [Smart Manufacturing Systems (SMS) Test Bed](https://www.nist.gov/laboratories/tools-instruments/smart-manufacturing-systems-sms-test-bed) offering Volatile Data Stream (VDS).

- [VDS Schema](https://smstestbed.nist.gov/vds)
- [Real-time stream of most current value for each data item](https://smstestbed.nist.gov/vds/current)
- [Time series of most recent values collected for each data item](https://smstestbed.nist.gov/vds/sample)
- [Report of all data items available](https://smstestbed.nist.gov/vds/probe)

## References

- [mtconnect.org](https://www.mtconnect.org/)
- [Github](http://www.github.com/mtconnect)
- [Schema](https://github.com/mtconnect/schema)
- [jaxb-ri](https://eclipse-ee4j.github.io/jaxb-ri/)
