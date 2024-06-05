import { FC } from 'react'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { CompiledValidateFunction } from '@rjsf/validator-ajv8/lib/types'

export enum BatchModeStepType {
  UPLOAD,
  MATCH,
  VALIDATE,
  CONFIRM,
}

export interface StepRendererProps {
  store: BatchModeStore
  onContinue: (partialStore: Partial<BatchModeStore>) => void
  onBatchUpload: (batch: Record<string, unknown>[]) => void
  onClose: () => void
}

export interface BatchModeSteps {
  id: BatchModeStepType
  title: string
  description: string
  renderer: FC<StepRendererProps>
  isFinal?: boolean
}

export interface BatchModeStore {
  schema: RJSFSchema
  fileName?: string
  worksheet?: WorksheetData[]
  mapping?: ColumnMappingData[]
  subscriptions?: ValidationColumns[]
}

export interface WorksheetData {
  [x: string]: unknown
}

export interface ColumnOption {
  value: string | number
  label: string
  type?: string
}

export interface ColumnMappingData {
  column: string
  subscription: string
}

type ErrorObject = Pick<CompiledValidateFunction, 'errors'>

export interface ValidationColumns extends ErrorObject {
  [x: string]: unknown
  row: number
  isError?: boolean
}
