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
 * Note that the stubs generated from the OpenAPI include schemas (see src/api/__generated__/schemas) but they are not usable
 * due to the lack of connection between the instances.
 * TheLibrary is not supported anymore; it needs replacement, see https://hivemq.kanbanize.com/ctrl_board/57/cards/24980/details/
 */
export const createSchema = (items: RJSFSchema) => {
  // TODO[NVL] This is total rubbish. The DeviceDataPoint schema should be self-extracted from the OpenAPI specs rather than second-guessed
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
        title: 'Tag Address',
        description: `The address of the data-point on the device.`,
        properties: sourceProperties,
      },
      DomainTag: {
        description: `A tag associated with a data point on a device connected to the adapter`,
        required: ['tag'],
        properties: {
          tag: {
            type: 'string',
            title: 'Tag Name',
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
        title: 'List of tags',
        description: 'The list of all tags defined in the device',
        items: {
          $ref: '#/definitions/DomainTag',
        },
      },
    },
  } as RJSFSchema
}
