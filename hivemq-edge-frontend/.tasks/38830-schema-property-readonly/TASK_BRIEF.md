# Task 38830: Schema Property Readonly

## Aims and Objectives

The task is to add a `readonly` property to the JSON Schema used in many part of the application to handle data mapping between PLC adapters and MQTT topic.

JSON Schema is used internally to represent the data payloads exchanged. In the frontend, they are abstracted in to a simple flat list of properties,
represented by the `FlatJSONSchema7`type. Properties are rendered visually in a consistent way, through the `PropertyItem` component.

The aim of the task is to extend the `FlatJSONSchema7` type to include the `readonly` property from JSON-Schema, which has been ignored.

The main impact of the change will be in the Southbound and Combiner/Asset mapper, where the destination properties (`MappingInstruction` component)
must not be mappable if they are marked as `readonly`.

The goals are:

- review the property handling functions (e.g. `getPropertyListFrom`) to ensure that readonly and other properties are handled. Extend unit tests when required
- extend the rendering of a property to clearly indicate a `readonly` status. Use icon and tooltip as appropriate
- Refactor the handling of mapping instruction to prevent interactions (edit, clear, drag-and-drop) with a `readonly` property.

## Background

<!-- Provide context and motivation -->

## Requirements

- The rendering of the `readonly` property MUST be clear and yet with minimal impact on the visual appearance of the property. There are already quite a few metadatas presented and we must convey both the importance of the `readonly` feature and its possible rare occurence
- In the mapping instruction interface, the `readonly` status of the DnD target MUST be be clearly indicated, visually and contextually. It must be consistent with the `required` or `isSupported` labelling

## Technical Notes

- src/components/rjsf/MqttTransformation/components/MappingInstructionList.tsx
- src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx
- src/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx
- src/components/rjsf/MqttTransformation/utils/json-schema.utils.ts
