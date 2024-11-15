import { FormContextType, IdSchema, RJSFValidationError } from '@rjsf/utils'

export type FormControlStore = {
  tabIndex: number
  setTabIndex: (n: number) => void
  clearController: () => void
}

export interface ChakraRJSFormContext extends FormContextType {
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  focusOnError?: (error: RJSFValidationError) => void
}
