# Plan: Fix XSD Auto-Generation for Backwards Compatibility

## Problem Summary

The current JAXB-generated XSD is **not backwards compatible** with existing configuration files due to three main issues:

1. **Element ordering**: JAXB generates `xs:sequence` (strict order) but configs use elements in arbitrary order
2. **Listener types**: Generated XSD doesn't properly express the listener type hierarchy (`tcp-listener`, `tls-tcp-listener`, `websocket-listener`, etc.)
3. **Protocol adapters**: Generated XSD expects `<protocol-adapter>` elements, but legacy configs use adapter-specific elements like `<simulation>`

## Solution Approach

Since JAXB has inherent limitations that cannot be overcome through annotations alone, the solution is to **post-process the generated XSD** to make it backwards compatible. This preserves the benefit of auto-generation while ensuring compatibility.

---

## Implementation Plan

### Phase 1: Enhance Post-Processing in GenSchemaMain.java

#### Task 1.1: Replace `xs:sequence` with `xs:all` for Root Entity

**Location**: `GenSchemaMain.java` - `addCustomSimpleTypes()` method (rename to `postProcessSchema()`)

**What to do**:
- Find `<xs:complexType name="hiveMQConfigEntity">`
- Replace `<xs:sequence>` with `<xs:all>`
- Replace closing `</xs:sequence>` with `</xs:all>`

**Why**: `xs:all` allows child elements in any order, which is how the manual XSD works and how users write configs.

**Limitation**: `xs:all` doesn't support `maxOccurs="unbounded"` on child elements, so wrapper elements like `mqtt-listeners` that contain lists must remain as `xs:sequence` internally.

#### Task 1.2: Fix Listener Type Polymorphism

**Location**: `GenSchemaMain.java` post-processing

**What to do**:
Replace the generated mqtt-listeners element:
```xml
<xs:element name="mqtt-listeners" minOccurs="0">
  <xs:complexType>
    <xs:sequence>
      <xs:element ref="address" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>
```

With a proper choice of listener types:
```xml
<xs:element name="mqtt-listeners" minOccurs="0">
  <xs:complexType>
    <xs:choice minOccurs="0" maxOccurs="unbounded">
      <xs:element ref="tcp-listener"/>
      <xs:element ref="tls-tcp-listener"/>
      <xs:element ref="websocket-listener"/>
      <xs:element ref="tls-websocket-listener"/>
    </xs:choice>
  </xs:complexType>
</xs:element>
```

**Why**: The Java code uses `@XmlElementRef` on `List<ListenerEntity>` which generates a reference to the abstract base, not the concrete subclasses.

#### Task 1.3: Fix Protocol Adapters to Use xs:any

**Location**: `GenSchemaMain.java` post-processing

**What to do**:
Replace the generated protocol-adapters element:
```xml
<xs:element name="protocol-adapters" minOccurs="0">
  <xs:complexType>
    <xs:sequence>
      <xs:element name="protocol-adapter" type="protocolAdapterEntity" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>
```

With:
```xml
<xs:element name="protocol-adapters" minOccurs="0">
  <xs:complexType>
    <xs:sequence>
      <xs:choice minOccurs="0" maxOccurs="unbounded">
        <xs:element name="protocol-adapter" type="protocolAdapterEntity"/>
        <xs:any processContents="skip" namespace="##other"/>
      </xs:choice>
    </xs:sequence>
  </xs:complexType>
</xs:element>
```

**Why**: Protocol adapters have dynamic schemas (each adapter type like `simulation`, `modbus`, `opcua` has its own structure). The `xs:any` allows legacy adapter-specific elements while `protocol-adapter` supports the new unified format.

#### Task 1.4: Fix Modules to Use xs:any

**Location**: `GenSchemaMain.java` post-processing

**What to do**:
Ensure modules element uses `xs:any`:
```xml
<xs:element name="modules" minOccurs="0">
  <xs:complexType>
    <xs:sequence>
      <xs:any processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>
```

**Why**: Modules are extensible and have arbitrary structures.

---

### Phase 2: Implementation Details

#### Task 2.1: Refactor GenSchemaMain.java

Create a new method structure:

```java
public class GenSchemaMain {

    public static void generateSchema(File outputFile) throws JAXBException, IOException {
        // 1. Generate base schema to temp file
        File tempFile = generateBaseSchema();

        // 2. Post-process for compatibility
        String schema = Files.readString(tempFile.toPath());
        schema = replaceSequenceWithAll(schema);
        schema = fixListenerTypes(schema);
        schema = fixProtocolAdapters(schema);
        schema = fixModules(schema);
        schema = addCustomSimpleTypes(schema);

        // 3. Write final schema
        Files.writeString(outputFile.toPath(), schema);
    }

    private static String replaceSequenceWithAll(String schema) {
        // Replace xs:sequence with xs:all in hiveMQConfigEntity
        // Must be careful to only replace the outer sequence, not nested ones
    }

    private static String fixListenerTypes(String schema) {
        // Replace address refs with proper listener type choices
    }

    private static String fixProtocolAdapters(String schema) {
        // Add xs:any for legacy adapter support
    }

    private static String fixModules(String schema) {
        // Ensure modules uses xs:any
    }
}
```

#### Task 2.2: Use XML DOM for Reliable Transformations

Instead of string manipulation, use proper XML DOM parsing:

```java
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

private static Document loadSchema(File file) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(file);
}

private static void transformSchema(Document doc) {
    // Find and modify specific elements using XPath or DOM traversal
}
```

**Why**: String replacement is fragile and error-prone. DOM manipulation is more reliable.

---

### Phase 3: Validation

#### Task 3.1: Create Validation Test

Add a test that validates all existing configs against the generated XSD:

```java
@Test
void generatedXsdShouldValidateAllExistingConfigs() throws Exception {
    File generatedXsd = new File("build/generated-xsd/config-generated.xsd");
    GenSchemaMain.generateSchema(generatedXsd);

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = factory.newSchema(generatedXsd);
    Validator validator = schema.newValidator();

    List<File> configFiles = findAllConfigFiles();
    for (File config : configFiles) {
        validator.validate(new StreamSource(config));
    }
}
```

#### Task 3.2: Add CI Check

Ensure the Gradle task runs as part of CI and validates:
1. Generated XSD is well-formed
2. All test configs validate against it
3. Generated XSD is consistent (no unexpected changes)

---

## Files to Modify

| File | Changes |
|------|---------|
| `hivemq-edge/src/test/java/com/hivemq/configuration/GenSchemaMain.java` | Major refactoring to add post-processing |
| `hivemq-edge/build.gradle.kts` | Add validation task (optional) |

## Files NOT to Modify

- Entity classes (`*Entity.java`) - The JAXB annotations are correct for runtime parsing
- Manual XSD (`config.xsd`) - Should eventually be replaced by generated one

---

## Testing Checklist

After implementation, verify these configs validate:
- [ ] `src/main/resources/config.xml`
- [ ] `src/test/resources/configs/simulation/*.xml`
- [ ] `src/test/resources/configs/testing/*.xml`
- [ ] `src/distribution/conf/examples/**/*.xml`

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| DOM manipulation could break valid XSD | Write comprehensive tests first |
| xs:all has limitations (no unbounded) | Keep xs:sequence for wrapper elements |
| Post-processing is complex | Use well-tested XML libraries |
| Schema changes could break existing users | Validate against all known config files |

---

## Future Considerations

1. **Schema versioning**: Consider adding version attribute to track schema evolution
2. **Deprecation**: Once generated XSD is proven, deprecate manual XSD maintenance
3. **Documentation**: Add XSD documentation annotations from Javadoc
