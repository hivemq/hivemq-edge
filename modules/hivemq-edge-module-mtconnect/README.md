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
| Type Cast        | Loose         | Strict              |

### Schema Validation Code Generation

- Download [jaxb-ri](https://eclipse-ee4j.github.io/jaxb-ri/) and unzip it to `modules/hivemq-edge-module-mtconnect` folder.
- Download [Xerces2 Java Binary (XML Schema 1.1)](https://xerces.apache.org/mirrors.cgi) and unzip it to `modules/hivemq-edge-module-mtconnect` folder.
- Add `xerces-2_12_2-xml-schema-1.1/xml-apis.jar` and `xerces-2_12_2-xml-schema-1.1/xercesImpl.jar` to the classpath.
- `git clone https://github.com/mtconnect/schema.git` to to `modules/hivemq-edge-module-mtconnect` folder. Here is a sample directory structure.

```
HiveMQ
└── hivemq-edge
    └── modules
        └── hivemq-edge-module-mtconnect
            ├── jaxb-ri
            ├── schema
            └── xerces-2_12_2-xml-schema-1.1
```

- Run all test cases in `MtConnectSchemaPatchTest` to generate the schema patch files. By default, those test cases are skipped if `schema` folder is not found.
- Navigate to `schema` and run `./patch-script.sh`.

- After all the schema files are generated, run all test cases `MtConnectSchemaJsonAnnotationTest` to convert the XML annotations to Jackson annotations. This is a one-time process and the test case is able to generate the Jackson annotations in an incremental way. By default, those test cases are skipped if `jaxb-ri` or `xerces` is not found.

#### Known Issues

##### Property Already Defined

```sh
parsing a schema...
[ERROR] Property "Type" is already defined. Use <jaxb:property> to resolve this conflict.
  line 5174 of file:/.../mtconnect/schema/MTConnectDevices_1.5.xsd

[ERROR] The following location is relevant to the above error
  line 5064 of file:/.../mtconnect/schema/MTConnectDevices_1.5.xsd
```

This issued is caused by the absence of property name in `<xs:attribute ref='xlink:type' use='optional' fixed='locator'>...</xs:attribute>`. So the default property name becomes `type` which conflicts with the inherited property name `type`. To resolve this issue, please follow the steps as follows.

1. Add a name to the attribute `<xs:attribute name='type1' ref='xlink:type' use='optional' fixed='locator'>...</xs:attribute>`.
2. Run the xjc command.
3. Replace `@XmlAttribute(name = "type1")` with `@XmlAttribute(name = "type")` in `mtconnect/schemas/devices/devices_1_5/DeviceRelationshipType.java`.

This fix applies to devices 1.5+.

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
