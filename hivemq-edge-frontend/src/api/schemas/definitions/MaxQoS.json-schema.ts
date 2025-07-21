import type { RJSFSchema } from '@rjsf/utils'

/* istanbul ignore next -- @preserve */
export const MaxQoS: RJSFSchema = {
  type: 'string',
  title: 'MQTT QoS',
  description: 'The maxQoS for this subscription',
  enum: ['0', '1', '2'],
  default: '0',
}
