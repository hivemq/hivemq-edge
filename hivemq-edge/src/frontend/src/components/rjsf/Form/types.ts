import type { FormContextType, IdSchema, RJSFValidationError } from '@rjsf/utils'

export type FormControlState = {
  tabIndex: number
  expandItems: string[]
}

export type FormControlAction = {
  reset: () => void
  setTabIndex: (n: number) => void
  setExpandItems: (items: string[]) => void
}

export type FormControlStore = FormControlState & FormControlAction

export interface ChakraRJSFormContext extends FormContextType {
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  focusOnError?: (error: RJSFValidationError) => void
}

export interface UITab {
  id: string
  title: string
  properties: string[]
}

export interface UITabIndexed extends UITab {
  index: number
}
