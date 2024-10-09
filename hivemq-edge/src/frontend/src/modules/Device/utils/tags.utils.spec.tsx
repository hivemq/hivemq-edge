import { describe, it, expect } from 'vitest'
import { createSchema, formatTagDataPoint } from '@/modules/Device/utils/tags.utils.ts'
import { MOCK_DEVICE_TAG_ADDRESS_MODBUS } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('formatTagDataPoint', () => {
  it('should return a warning when no data schema given', () => {
    expect(formatTagDataPoint()).toBe('< unknown format >')
  })

  it('should return a formatted payload', () => {
    expect(formatTagDataPoint(MOCK_DEVICE_TAG_ADDRESS_MODBUS)).toBe(
      JSON.stringify(MOCK_DEVICE_TAG_ADDRESS_MODBUS, null, 4)
    )
  })
})

describe('createSchema', () => {
  it('should return a wrong schema if items not defined', () => {
    expect(createSchema({ fakeProperty: 'my-property' })).toStrictEqual(
      expect.objectContaining({
        definitions: expect.objectContaining({
          DeviceDataPoint: expect.objectContaining({
            properties: {},
          }),
          DomainTag: expect.objectContaining({}),
        }),
        properties: expect.objectContaining({}),
      })
    )
  })

  it('should return a correct schema', () => {
    expect(
      createSchema({
        properties: {
          fake: {
            type: 'string',
          },
        },
      })
    ).toStrictEqual(
      expect.objectContaining({
        definitions: expect.objectContaining({
          DeviceDataPoint: expect.objectContaining({
            properties: {
              fake: {
                type: 'string',
              },
            },
          }),
          DomainTag: expect.objectContaining({}),
        }),
        properties: expect.objectContaining({}),
      })
    )
  })

  it('should return a filtered schema', () => {
    expect(
      createSchema({
        properties: {
          fake: {
            type: 'string',
          },
          mqttTopic: {
            type: 'string',
          },
        },
      })
    ).toStrictEqual(
      expect.objectContaining({
        definitions: expect.objectContaining({
          DeviceDataPoint: expect.objectContaining({
            properties: {
              fake: {
                type: 'string',
              },
            },
          }),
          DomainTag: expect.objectContaining({}),
        }),
        properties: expect.objectContaining({}),
      })
    )
  })
})
