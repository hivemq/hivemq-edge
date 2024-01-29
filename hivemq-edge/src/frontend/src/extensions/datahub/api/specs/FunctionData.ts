import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/FunctionData.json'

export const MOCK_FUNCTION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    type: {
      'ui:widget': 'hidden',
    },
    name: {
      'ui:widget': 'datahub:function-name',
    },
  },
}
