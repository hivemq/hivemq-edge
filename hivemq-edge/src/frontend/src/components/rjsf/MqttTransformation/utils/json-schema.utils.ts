import type { RJSFSchema } from '@rjsf/utils'
import type { JSONSchema7, JSONSchema7Definition } from 'json-schema'
import type { JsonNode } from '@/api/__generated__'
import { match, P } from 'ts-pattern'
import { inferSchema } from '@jsonhero/schema-infer'
import type { MQTTSample } from '@/hooks/usePrivateMqttClient/type.ts'

export const ARRAY_ITEM_INDEX = '___index'

export interface FlatJSONSchema7 extends JSONSchema7 {
  path: string[]
  key: string
  arrayType?: string
}

export const getProperty = (
  key: string,
  path: string[],
  property: JSONSchema7Definition,
  definitions: { [p: string]: JSONSchema7Definition } | undefined
): FlatJSONSchema7[] => {
  let tempProperty = property

  // check if it's a ref to a definition
  const { $ref: ref, title } = property as JSONSchema7
  if (ref) {
    // We only support the local definitions, i.e. with pattern "#/definitions/{property}"
    const defPath = ref.split('/')
    const defName = defPath.pop()
    if (ref.startsWith('#') && defName && definitions?.[defName]) {
      tempProperty = definitions?.[defName]
    } else {
      // This is an error, ref defined but definition not found.
      // No error in JSONSchema so using null type and description
      return [{ key: key, path: path, type: 'null', description: `error: definition ${defName} doesn't exist` }]
    }
  }

  // Add the property
  const { type, description, title: titleRef, examples } = tempProperty as JSONSchema7
  const mainProps: FlatJSONSchema7 = { key, path, type, description, title: titleRef || title || key, examples }

  const subProperties: FlatJSONSchema7[] = []
  if (type === 'object') {
    // Check recursively for object's properties
    const { properties } = tempProperty as JSONSchema7
    if (properties) {
      for (const [subKey, subProp] of Object.entries(properties)) {
        subProperties.push(...getProperty(subKey, [...path, key], subProp, definitions))
      }
    }
  } else if (type === 'array') {
    const { items } = tempProperty as JSONSchema7
    const { type: arrayType } = items as JSONSchema7
    if (arrayType) mainProps.arrayType = Array.isArray(arrayType) ? arrayType[0] : arrayType
  }

  return [mainProps, ...subProperties]
}

export const getPropertyListFrom = (schema: RJSFSchema): FlatJSONSchema7[] => {
  const { properties, definitions } = schema

  const flatList: FlatJSONSchema7[] = []
  if (!properties) return flatList

  for (const [key, property] of Object.entries(properties)) {
    const results = getProperty(key, [], property, definitions)
    flatList.push(...results)
  }
  return flatList
}

export const reducerSchemaExamples = (state: RJSFSchema, event: JsonNode) =>
  match([state, event])
    .returnType<RJSFSchema>()
    .with(
      [{ type: 'object', properties: P.select('properties') }, P.select('values')],
      ({ properties, values }, [subSchema]) => {
        if (!properties) return subSchema

        const allPropertyNames = Object.keys(properties)
        const newProperties: RJSFSchema = {}
        for (const propName of allPropertyNames) {
          newProperties[propName] = reducerSchemaExamples(properties[propName] as RJSFSchema, values?.[propName])
        }
        return { ...subSchema, properties: newProperties }
      }
    )
    .with([{ type: 'array' }, P.select('values')], (_, [subSchema]) => {
      return subSchema
    })
    .with(P._, ([allStates, examples]) => {
      if (!examples) return allStates
      return { ...allStates, examples: [examples] }
    })
    .exhaustive()

export const payloadToSchema = (samples: MQTTSample[] | undefined) => {
  const results: Record<string, RJSFSchema> = {}
  for (const { topic, payload } of samples || []) {
    const inference = inferSchema(payload)
    // The inference process doesn't deal with examples; values need a second pattern processing
    const schema = inference.toJSONSchema() as RJSFSchema
    results[topic] = reducerSchemaExamples(schema, payload)
  }
  return results
}
