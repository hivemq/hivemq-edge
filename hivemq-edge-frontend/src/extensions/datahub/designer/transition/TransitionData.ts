/* istanbul ignore file -- @preserve */
import type { PanelSpecs } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'

const schema: RJSFSchema = {
  type: 'object',
  required: ['model', 'event'],
  properties: {
    model: {
      type: 'string',
      title: 'Behavior Model',
      description: 'The behavior model selected for this policy',
    },
    event: {
      type: 'string',
      title: 'Transition',
      description:
        'The movement of the MQTT client from one state to another. Each transition consists of a from state, a to state and a specific event',
    },
    from: { type: 'string' },
    to: { type: 'string' },
  },
}

export const MOCK_TRANSITION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    model: {
      'ui:readonly': true,
      'ui:widget': 'datahub:behavior-model-readonly',
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
