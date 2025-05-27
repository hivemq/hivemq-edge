/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'
import schema from '@datahub/api/__generated__/schemas/DataPolicyValidator.json'

export const MOCK_VALIDATOR_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
}
