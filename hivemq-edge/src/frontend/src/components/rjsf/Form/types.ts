import { FormContextType, IdSchema, RJSFValidationError } from '@rjsf/utils'

export interface ChakraRJSFormContext extends FormContextType {
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  focusOnError?: (error: RJSFValidationError) => void
}
