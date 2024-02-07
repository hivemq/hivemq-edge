import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

const schema: RJSFSchema = {
  type: 'object',
  required: ['model', 'transition'],
  properties: {
    model: { type: 'string' },
    transition: { type: 'string' },
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
    transition: {
      'ui:widget': 'datahub:transition-selector',
    },
  },
}
