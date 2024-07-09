/* istanbul ignore file -- @preserve */
import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub//api/__generated__/schemas/ClientFilterData.json'

export const MOCK_CLIENT_FILTER_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    clients: {
      'ui:options': {
        orderable: false,
      },
    },
  },
}
