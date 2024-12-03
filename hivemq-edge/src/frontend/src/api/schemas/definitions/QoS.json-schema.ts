import { RJSFSchema } from '@rjsf/utils'
import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'

/* istanbul ignore next -- @preserve */
export const QoS: RJSFSchema = {
  type: 'number',
  title: 'MQTT QoS',
  description: 'MQTT Quality of Service level',
  // TODO[NVL] current way of doing MaxQOS: wrong unvalidated content
  default: MOCK_MAX_QOS,
  // TODO[NVL] First way of doing enums: enum + ui:enumNames
  // enum: [0, 1, 2],
  // TODO[NVL] Second way of doing enums: enum + enumNames (deprecated, not valid extension of JSONSchema)
  // enumNames: ['At most once (QoS 0)', 'At least once (QoS 1)', 'Exactly once (QoS 2)'],
  // TODO[NVL] Third way of doing enums: oneOf (error messages are not exactly user friendly)
  // oneOf: [
  //   { const: 0, title: 'At most once (QoS 0)' },
  //   { const: 1, title: 'At least once (QoS 1)' },
  //   { const: 2, title: 'Exactly once (QoS 2)' },
  // ],
  // TODO[NVL] Fourth way of doing enums: anyOf (error messages are not exactly user friendly)
  // anyOf: [
  //   {
  //     type: 'number',
  //     title: 'At most once (QoS 0)',
  //     enum: [1],
  //   },
  //   {
  //     type: 'number',
  //     title: 'At least once (QoS 1)',
  //     enum: [2],
  //   },
  //   {
  //     type: 'number',
  //     title: 'Exactly once (QoS 2)',
  //     enum: [3],
  //   },
  // ],
}
