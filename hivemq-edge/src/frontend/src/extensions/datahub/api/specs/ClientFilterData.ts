import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { StrictRJSFSchema } from '@rjsf/utils/src/types.ts'
import schema from '../__generated__/schemas/ClientFilterData.json'

export const MOCK_CLIENT_FILTER_SCHEMA: PanelSpecs = {
  schema: schema as StrictRJSFSchema,
  uiSchema: {
    clients: {
      'ui:options': {
        orderable: false,
      },
    },
  },
}
