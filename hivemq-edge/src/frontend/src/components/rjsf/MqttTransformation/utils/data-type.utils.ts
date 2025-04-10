import type { JSONSchema7TypeName } from 'json-schema'
import type { IconType } from 'react-icons'
import { MdDataObject, MdNumbers, MdOutlineDataArray, MdOutlineTextFields, MdQuestionMark } from 'react-icons/md'
import { RxComponentBoolean } from 'react-icons/rx'
import { TbDecimal } from 'react-icons/tb'

import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

export const DataTypeIcon: Record<JSONSchema7TypeName, IconType> = {
  ['object']: MdDataObject,
  ['string']: MdOutlineTextFields,
  ['array']: MdOutlineDataArray,
  ['number']: TbDecimal,
  ['boolean']: RxComponentBoolean,
  ['integer']: MdNumbers,
  ['null']: MdQuestionMark,
}

export const isMappingSupported = (property: FlatJSONSchema7) => {
  return property.type != undefined && property.type !== 'object'
}

export const filterSupportedProperties = (property: FlatJSONSchema7) => Boolean(property.path.length === 0)

export const formatPath = (path: string) => path.replaceAll('.', '.​')

export const toJsonPath = (property: string) => {
  // Not sure about the empty string case
  if (property === '') return '$'
  if (property.startsWith('$.')) return property
  return `$.${property}`
}

export const fromJsonPath = (path: string) => {
  if (path === '') return ''
  if (path === '$') return ''
  if (path.startsWith('$.')) return path.slice(2)
  return path
}
