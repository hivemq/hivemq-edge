# MQTT to  OPC_UA

## Data Types

| OPC_UA Data Type | Java Data Type  | Json Data Type        | Example                   |
 |------------------|-----------------|-----------------------|---------------------------|
| Boolean          | Boolean         | Boolean               | {"value":true}            |
| Byte             | UByte           | Base64-encoded-string | {"value": "TODO"}         |
| SByte            | Byte            | Base64-encoded-string | {"value": "TODO"}         |
| Int16            | Short           | number (integral)     | {"value": 1337 }          |
| UInt16           | UShort          | number (integral)     | {"value": 1337 }          |
| Int32            | Integer         | number (integral)     | {"value": 1337 }          |
| UInt32           | UInteger        | number (integral)     | {"value": 1337 }          |
| Int64            | Long            | number (integral)     | {"value": 1337 }          |
| UInt64           | ULong           | number (integral)     | {"value": 1337 }          |
| Float            | Float           |
| Double           | Double          |
| String           | String          | string                | {"value": "hello world" } |
| DateTime         | DateTime        |
| Guid             | UUID            |
| ByteString       | ByteString      |
| XmlElement       | XmlElement      |
| NodeId           | NodeId          |
| ExpandedNodeId   | ExpandedNodeId  |
| StatusCode       | StatusCode      |
| QualifiedName    | QualifiedName   |
| LocalizedText    | LocalizedText   |
| DataValue        | not implemented |
| Variant          | not implemented |
| DiagnosticInfo   | not implemented |
| ExtensionObject  | Struct          |
