import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/BehaviorPolicyData.json'

export const MOCK_BEHAVIOR_POLICY_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
}
