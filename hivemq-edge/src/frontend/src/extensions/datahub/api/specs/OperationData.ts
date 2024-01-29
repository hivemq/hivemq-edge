import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/OperationData.json'

export const MOCK_OPERATION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
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
