import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { StrictRJSFSchema } from '@rjsf/utils/src/types.ts'

import schema from '../__generated__/schemas/BehaviorPolicyData.json'

export const MOCK_BEHAVIOR_POLICY_SCHEMA: PanelSpecs = {
  schema: schema as StrictRJSFSchema,
}
