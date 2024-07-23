# MQTT to  OPC_UA

## Data Types

| OPC_UA Data Type | Java Data Type  | Json Data Type                      | Example                                                                                                                  |
 |------------------|-----------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Boolean          | Boolean         | Boolean                             | {"value":true}                                                                                                           |
| Byte             | UByte           | Base64-encoded-string (single byte) | {"value": "YQ=="}                                                                                                        |
| SByte            | Byte            | Base64-encoded-string (single byte) | {"value": "YQ=="}                                                                                                        |
| Int16            | Short           | number (integral)                   | {"value": 1337 }                                                                                                         |
| UInt16           | UShort          | number (integral)                   | {"value": 1337 }                                                                                                         |
| Int32            | Integer         | number (integral)                   | {"value": 1337 }                                                                                                         |
| UInt32           | UInteger        | number (integral)                   | {"value": 1337 }                                                                                                         |
| Int64            | Long            | number (integral)                   | {"value": 1337 }                                                                                                         |
| UInt64           | ULong           | number (integral)                   | {"value": 1337 }                                                                                                         |
| Float            | Float           | number (floating point)             | {"value": 3.14 }                                                                                                         |
| Double           | Double          | number (floating point)             | {"value": 3.14 }                                                                                                         |
| String           | String          | string                              | {"value": "hello world" }                                                                                                |
| DateTime         | DateTime        | number (integral)                   | {"value": 133662065709780000}                                                                                            |
| Guid             | UUID            | string                              | {"value": "f5fef123-a8e1-4b0c-b338-41fd33cba0bd"}                                                                        |
| ByteString       | ByteString      | Base64-encoded-string               | {"value": "SGVsbG8gV29ybGQ="}                                                                                            |
| XmlElement       | XmlElement      | string                              | {"value": "Hello World"}                                                                                                 |
| NodeId           | NodeId          | string                              | {"value": "ns=2;s=AddressCustomType"}                                                                                    |
| ExpandedNodeId   | ExpandedNodeId  | string                              | {"value": "nsu=urn:hivemq:test:testns;s=AddressCustomType"}                                                              |
| StatusCode       | StatusCode      | number (integral)                   | {"value": 1 }                                                                                                            |
| QualifiedName    | QualifiedName   | object                              | {"value": {"namespaceIndex": 1, "name": "someName"} }                                                                    |
| LocalizedText    | LocalizedText   | string                              | {"value": "Hello World"}                                                                                                 |
| DataValue        | not implemented | n/a                                 | n/a                                                                                                                      |
| Variant          | not implemented | n/a                                 | n/a                                                                                                                      |
| DiagnosticInfo   | not implemented | n/a                                 | n/a                                                                                                                      |
| ExtensionObject  | Struct          | object                              | {"value":{"Name":"foo-value","Age":2,"Address":{"StreetName":"foo-value","Number":1,"Sold":true,"BuildingCondition":1}}} |
