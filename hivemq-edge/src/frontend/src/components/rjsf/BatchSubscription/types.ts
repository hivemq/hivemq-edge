import { FC } from 'react'

export enum BatchModeStep {
  UPLOAD,
  MATCH,
  VALIDATE,
  CONFIRM,
}

export interface StepProps {
  onContinue: (partialStore: BatchModeStore) => void
}

export interface BatchModeSteps {
  id: BatchModeStep
  title: string
  description: string
  renderer: FC<StepProps>
  isFinal?: boolean
}

export interface BatchModeStore {
  worksheet?: WorksheetData[]
}

export interface WorksheetData {
  [x: string]: unknown
  __rowNum__: number
}
