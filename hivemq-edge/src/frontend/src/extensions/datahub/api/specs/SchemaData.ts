import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/SchemaData.json'

export const MOCK_SCHEMA_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    type: {
      // 'ui:widget': 'radio',
    },
  },
}
