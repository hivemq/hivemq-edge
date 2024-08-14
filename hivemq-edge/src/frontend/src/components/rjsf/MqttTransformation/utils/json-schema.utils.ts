import { RJSFSchema } from '@rjsf/utils'
import { JSONSchema7 } from 'json-schema'

export interface FlatJSONSchema7 extends JSONSchema7 {
  path: string[]
}

export const getPropertyListFrom = (schema: RJSFSchema): FlatJSONSchema7[] => {
  const { properties, definitions } = schema
  if (!properties) return []
  const list: FlatJSONSchema7[] = []

  for (const [key, property] of Object.entries(properties)) {
    const { type, description, title, $ref: ref } = property as JSONSchema7
    if (ref) {
      const defPath = ref.split('/')
      const defName = defPath.pop()
      if (defName) {
        list.push({ title: title || key, type: 'object', description, path: [] })
        if (definitions?.[defName]) {
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
      if (type === 'array') {
        const { items } = property as JSONSchema7
        if (items) {
          const { properties: subs } = items as FlatJSONSchema7
          if (subs)
            for (const [key, value] of Object.entries(subs)) {
              const { type: type2, title: title2 } = value as JSONSchema7
              list.push({ title: title2 || key, type: type2, path: [title || ''] })
            }
        }
      }
    }
  }
  return list
}
