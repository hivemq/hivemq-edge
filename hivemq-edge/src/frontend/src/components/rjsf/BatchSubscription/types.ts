import { FC } from 'react'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

export enum BatchModeStepType {
  UPLOAD,
  MATCH,
  VALIDATE,
  CONFIRM,
}

export interface StepRendererProps {
  store: BatchModeStore
  onContinue: (partialStore: Partial<BatchModeStore>) => void
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
}

export interface WorksheetData {
  [x: string]: unknown
  __rowNum__: number
}

export interface ColumnOption {
  value: string | number
  label: string
  type?: string
}
