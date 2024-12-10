import { RJSFSchema } from '@rjsf/utils'
import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'
import { QoS as QoSEnum } from '@/api/__generated__'

/* istanbul ignore next -- @preserve */
export const QoS: RJSFSchema = {
  type: 'string',
  title: 'MQTT QoS',
  description: 'MQTT Quality of Service level',
  enum: [QoSEnum.AT_MOST_ONCE, QoSEnum.AT_LEAST_ONCE, QoSEnum.EXACTLY_ONCE],
  default: MOCK_MAX_QOS,
}
