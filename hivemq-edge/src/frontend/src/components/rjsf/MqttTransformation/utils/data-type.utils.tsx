import type { JSONSchema7TypeName } from 'json-schema'
import type { IconType } from 'react-icons'
import { MdDataObject, MdNumbers, MdOutlineDataArray, MdOutlineTextFields, MdQuestionMark } from 'react-icons/md'
import { RxComponentBoolean } from 'react-icons/rx'
import { TbDecimal } from 'react-icons/tb'

export const DataTypeIcon: Record<JSONSchema7TypeName, IconType> = {
  ['object']: MdDataObject,
  ['string']: MdOutlineTextFields,
  ['array']: MdOutlineDataArray,
  ['number']: TbDecimal,
  ['boolean']: RxComponentBoolean,
  ['integer']: MdNumbers,
  ['null']: MdQuestionMark,
}
