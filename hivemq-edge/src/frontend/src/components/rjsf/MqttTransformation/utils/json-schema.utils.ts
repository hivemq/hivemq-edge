import { RJSFSchema } from '@rjsf/utils'
import { JSONSchema7 } from 'json-schema'

export const ARRAY_ITEM_INDEX = '___index'

export interface FlatJSONSchema7 extends JSONSchema7 {
  path: string[]
  key: string
}

export const getPropertyListFrom = (schema: RJSFSchema): FlatJSONSchema7[] => {
  const { properties, definitions } = schema
  if (!properties) return []
  const list: FlatJSONSchema7[] = []

  // TODO[NVL] This is incorrect! It should be recursive (array, object)
  for (const [key, property] of Object.entries(properties)) {
    const { type, description, title, $ref: ref } = property as JSONSchema7
    if (ref) {
      const defPath = ref.split('/')
      const defName = defPath.pop()
      if (defName) {
        list.push({ title: title || key, type: 'object', description, path: [] })
        if (definitions?.[defName]) {
          // TODO[NVL] This is incorrect! It should use the same path as below
          const { properties: subs } = definitions[defName] as JSONSchema7
          if (subs)
            for (const [key, value] of Object.entries(subs)) {
              const { type: type2, title: title2 } = value as JSONSchema7
              list.push({ title: title2 || key, type: type2, path: [title || ''] })
            }
        }
      }
    } else {
      list.push({ title: title || key, type, description, path: [] })
      if (type === 'object') {
        const { properties } = property as JSONSchema7
        if (properties) {
          for (const [key, value] of Object.entries(properties)) {
            const { type: type2, title: title2 } = value as JSONSchema7
            list.push({ title: title2 || key, type: type2, path: [title || ''] })
          }
        }
      }
      if (type === 'array') {
        const { items } = property as JSONSchema7
        if (items) {
          const { properties: subs, type: arrayItemType } = items as FlatJSONSchema7
          if (subs)
            for (const [key, value] of Object.entries(subs)) {
              const { type: type2, title: title2 } = value as JSONSchema7
              list.push({ title: title2 || key, type: type2, path: [title || ''] })
            }
          else {
            list.push({ title: ARRAY_ITEM_INDEX || key, type: arrayItemType, path: [title || ''] })
          }
        }
      }
    }
  }
  return list
}
