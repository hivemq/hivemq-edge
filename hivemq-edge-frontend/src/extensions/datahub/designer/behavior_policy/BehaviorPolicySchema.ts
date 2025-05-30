/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub/api/__generated__/schemas/BehaviorPolicyData.json'

export const MOCK_BEHAVIOR_POLICY_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    arguments: {
      minPublishes: {
        'ui:widget': 'updown',
      },
      maxPublishes: {
        'ui:widget': 'radio', // Hide the enum field since it's fixed
        // This is to reflect the use of oneOf on the definition of maxPublish
        oneOf: [
          {
            'ui:widget': 'hidden', // Hide the enum field since it's fixed
          },
          {
            // must be there for the second element,
          },
        ],
      },
    },
  },
}
