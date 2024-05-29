import { FC } from 'react'

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
  worksheet?: WorksheetData[]
}

export interface WorksheetData {
  [x: string]: unknown
  __rowNum__: number
}
