/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

const schema: RJSFSchema = {
  type: 'object',
  required: ['model', 'event'],
  properties: {
    model: { type: 'string' },
    event: { type: 'string' },
    from: { type: 'string' },
    to: { type: 'string' },
  },
}

export const MOCK_TRANSITION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    model: {
      'ui:readonly': true,
    },
    from: {
      'ui:readonly': true,
      'ui:widget': 'hidden',
    },
    to: {
      'ui:readonly': true,
      'ui:widget': 'hidden',
    },
    event: {
      'ui:widget': 'datahub:transition-selector',
    },
  },
}
