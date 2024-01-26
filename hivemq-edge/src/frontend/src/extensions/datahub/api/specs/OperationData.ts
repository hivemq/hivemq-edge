import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { StrictRJSFSchema } from '@rjsf/utils/src/types.ts'

import schema from '../__generated__/schemas/OperationData.json'

export const MOCK_OPERATION_SCHEMA: PanelSpecs = {
  schema: schema as StrictRJSFSchema,
  uiSchema: {
    functionId: {
      'ui:widget': 'datahub:function-selector',
    },
    formData: {
      message: {
        'ui:widget': 'textarea',
      },
      incrementBy: {
        'ui:widget': 'updown',
      },
      metricName: {
        'ui:widget': 'datahub:metric-counter',
      },
    },
  },
}
