import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { StrictRJSFSchema } from '@rjsf/utils/src/types.ts'
import schema from '../__generated__/schemas/DataPolicyValidator.json'

export const MOCK_VALIDATOR_SCHEMA: PanelSpecs = {
  schema: schema as StrictRJSFSchema,
}
