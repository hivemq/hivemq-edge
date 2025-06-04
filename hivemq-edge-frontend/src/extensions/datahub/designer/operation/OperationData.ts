/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub/api/__generated__/schemas/OperationData.json'

export const MOCK_OPERATION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    functionId: {
      'ui:widget': 'datahub:function-selector',
    },

    formData: {
      transform: {
        'ui:options': {
          readonly: true,
          removable: false,
          addable: false,
        },
      },
      message: {
        'ui:widget': 'datahub:message-interpolation',
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
