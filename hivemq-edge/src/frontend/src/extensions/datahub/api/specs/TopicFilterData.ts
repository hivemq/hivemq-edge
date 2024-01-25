import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/TopicFilterData.json'

export const MOCK_TOPIC_FILTER_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    topics: {
      'ui:options': {
        // TODO[NVL] Reordering the topic reorders the handles and edges are gone
        orderable: false,
      },
    },
  },
}
