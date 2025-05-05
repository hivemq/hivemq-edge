import { describe, expect } from 'vitest'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { BehaviorPolicyType } from '@datahub/types.ts'

describe('enumFromStringValue', () => {
  it('should convert acceptable placeholders to HTML markup', async () => {
    expect(enumFromStringValue(BehaviorPolicyType, 'fake')).toBeUndefined()
    expect(enumFromStringValue(BehaviorPolicyType, 'Mqtt.events')).toEqual(BehaviorPolicyType.MQTT_EVENT)
  })
})
