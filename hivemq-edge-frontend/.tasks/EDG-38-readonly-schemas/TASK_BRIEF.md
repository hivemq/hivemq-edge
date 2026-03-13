# EDG-38 — Frontend should ignore read-only information for topic-filter schemas and data-combining destination schemas

## Linear Issue

<https://linear.app/hivemq/issue/EDG-38>

## Background (from Linear)

The `readOnly` flag in JSON schemas describes whether a Java object field can be written during
deserialization. This is **not** relevant in two specific frontend contexts:

- **Topic-filter sources**: we are only _reading_ values from a JSON object; the readOnly attribute
  is meaningless.
- **Data-combining destinations**: we are _assembling_ a new combined message (serialization), not
  deserializing; the readOnly attribute is again meaningless.

Therefore, in those contexts the readOnly information should be **completely ignored**.

## Additional Context (from user)

The visual element in question is the **lock icon** rendered by `PropertyItem` (the "flattened"
property renderer) when `property.readOnly === true`. The combiner mapping editor already
correctly handles the readOnly case. The task is to make the lock icon (and any related readOnly
logic) **conditional** across the many contexts where `PropertyItem` / `JsonSchemaBrowser` are
used, with minimal refactoring.

## Components Involved

| Component                | File                                                           | Renders readOnly?             |
| ------------------------ | -------------------------------------------------------------- | ----------------------------- |
| `PropertyItem`           | `MqttTransformation/components/schema/PropertyItem.tsx`        | Yes — lock icon               |
| `JsonSchemaBrowser`      | `MqttTransformation/JsonSchemaBrowser.tsx`                     | Via `PropertyItem`            |
| `MappingInstruction`     | `MqttTransformation/components/mapping/MappingInstruction.tsx` | Yes — blocking card           |
| `MappingInstructionList` | `MqttTransformation/components/MappingInstructionList.tsx`     | Filters readOnly instructions |

## Callers of `JsonSchemaBrowser` / `PropertyItem`

| Caller                 | Context                                              | Should show readOnly? |
| ---------------------- | ---------------------------------------------------- | --------------------- |
| `TagSchemaPanel`       | Device tag write schema                              | **YES**               |
| `TopicSchemaManager`   | Topic filter schema browser                          | No                    |
| `SchemaSampler`        | Topic filter schema sampler                          | No                    |
| `MetadataExplorer`     | Domain ontology topic explorer                       | **YES**               |
| `DataModelSources`     | MQTT transformation source panel                     | No                    |
| `DataModelDestination` | MQTT transformation destination panel                | No                    |
| `CombinedSchemaLoader` | Combiner source schema browser                       | No                    |
| `SchemaMerger`         | Combiner schema infer (uses `PropertyItem` directly) | No                    |
| `SchemaWidget`         | RJSF widget showing a topic filter schema            | No                    |
