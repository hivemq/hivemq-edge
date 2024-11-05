import { FormContextType, IdSchema } from '@rjsf/utils'

export interface ChakraRJSFormContext extends FormContextType {
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
}
