/* istanbul ignore file -- @preserve */
import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub/api/__generated__/schemas/BehaviorPolicyData.json'

export const MOCK_BEHAVIOR_POLICY_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    arguments: {
      minPublishes: {
        'ui:widget': 'updown',
      },
      maxPublishes: {
        'ui:widget': 'updown',
      },
    },
  },
}
