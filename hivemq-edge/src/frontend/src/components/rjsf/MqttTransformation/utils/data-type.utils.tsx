import type { JSONSchema7TypeName } from 'json-schema'
import type { IconType } from 'react-icons'
import {
  MdDataObject,
  MdNumbers,
  MdOutlineDataArray,
  MdOutlineNumbers,
  MdOutlineTextFields,
  MdQuestionMark,
} from 'react-icons/md'
import { RxComponentBoolean } from 'react-icons/rx'

export const DataTypeIcon: Record<JSONSchema7TypeName, IconType> = {
  ['object']: MdDataObject,
  ['string']: MdOutlineTextFields,
  ['array']: MdOutlineDataArray,
  ['number']: MdOutlineNumbers,
  ['boolean']: RxComponentBoolean,
  ['integer']: MdNumbers,
  ['null']: MdQuestionMark,
}
