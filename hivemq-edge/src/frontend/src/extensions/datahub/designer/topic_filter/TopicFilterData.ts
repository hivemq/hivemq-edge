/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

import schema from '@datahub/api/__generated__/schemas/TopicFilterData.json'

export const MOCK_TOPIC_FILTER_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    adapter: {
      'ui:widget': 'edge:adapter-selector',
    },
    topics: {
      'ui:options': {
        // TODO[NVL] Reordering the topic reorders the handles and edges are gone
        orderable: false,
      },
    },
  },
}
