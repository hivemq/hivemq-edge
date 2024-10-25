import type { JSONSchema7TypeName } from 'json-schema'
import type { IconType } from 'react-icons'
import { MdDataObject, MdNumbers, MdOutlineDataArray, MdOutlineTextFields, MdQuestionMark } from 'react-icons/md'
import { RxComponentBoolean } from 'react-icons/rx'
import { TbDecimal } from 'react-icons/tb'

import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

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
  return property.type !== 'object'
}

export const filterSupportedProperties = (property: FlatJSONSchema7) => Boolean(property.path.length === 0)

export const formatPath = (path: string) => path.replaceAll('.', '.​')
