/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub/api/__generated__/schemas/SchemaData.json'

export const MOCK_SCHEMA_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
}
