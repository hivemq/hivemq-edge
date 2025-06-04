import type { FC } from 'react'
import type { CompiledValidateFunction } from '@rjsf/validator-ajv8'
import type { RJSFSchema } from '@rjsf/utils'
import type { IdSchema } from '@rjsf/utils'

export enum BatchModeStepType {
  UPLOAD,
  MATCH,
  VALIDATE,
  CONFIRM,
}

export interface StepRendererProps {
  store: BatchModeStore
  onContinue: (partialStore: Partial<BatchModeStore>) => void
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  onClose?: () => void
}

export interface BatchModeSteps {
  id: BatchModeStepType
  title: string
  description: string
  renderer: FC<StepRendererProps>
  isFinal?: boolean
}

export interface BatchModeStore {
  idSchema: IdSchema<unknown>
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

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ErrorObject {
  export enum keyword {
    REQUIRED = 'required',
  }
}
