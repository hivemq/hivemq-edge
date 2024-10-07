import { DeviceDataPoint, JsonNode } from '@/api/__generated__'
import { RJSFSchema } from '@rjsf/utils'

export const formatTagDataPoint = (data?: DeviceDataPoint) => {
  if (data) return JSON.stringify(data, null, 4)
  return '< unknown format >'
}

const omit = (obj: JsonNode, ...props: string[]) => {
  const result = { ...obj }
  props.forEach(function (prop) {
    delete result[prop]
  })
  return result
}

/**
 * @deprecated This is a mock, missing support for tags
 */
export const createSchema = (items: RJSFSchema) => {
  // This is totally wrong. The DeviceDataPoint schema should be clearly indicated in the
  const sourceProperties = omit(
    items.properties as JsonNode,
    'mqttQos',
    'mqttTopic',
    'messageExpiryInterval',
    'publishingInterval',
    'serverQueueSize',
    'includeTagNames',
    'includeTimestamp',
    'messageHandlingOptions',
    'mqttUserProperties'
  )

  return {
    // $schema: 'https://json-schema.org/draft/2020-12/schema',
    definitions: {
      DeviceDataPoint: {
        type: 'object',
        properties: sourceProperties,
      },
      DomainTag: {
        description: `A tag associated with a data point on a device connected to the adapter`,
        required: ['tag'],
        properties: {
          tag: {
            type: 'string',
            description: `The Tag associated with the data-point.`,
          },
          dataPoint: {
            $ref: '#/definitions/DeviceDataPoint',
          },
        },
      },
    },
    properties: {
      tags: {
        type: 'array',
        items: {
          $ref: '#/definitions/DomainTag',
        },
      },
    },
  } as RJSFSchema
}
