import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'
import schema from '../__generated__/schemas/DataPolicyValidator.json'

export const MOCK_VALIDATOR_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
}
