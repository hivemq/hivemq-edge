import type { JSONSchema7 } from 'json-schema'
import type { Dispatch, SetStateAction } from 'react'

export type CustomPropertyValue = string
export type FormDataItem = Record<string, CustomPropertyValue>
export type CustomPropertyForm = FormDataItem[]

export interface DataGridProps {
  data: CustomPropertyForm
  columnTypes: [string, JSONSchema7][]
  isDisabled?: boolean
  required?: string[]
  maxItems?: number
  onHandleDeleteItem?: (index: number) => void
  onHandleAddItem?: () => void
  onUpdateData?: (rowIndex: number, columnId: string, value: CustomPropertyValue) => void
  onSetData?: Dispatch<SetStateAction<CustomPropertyForm>>
}
