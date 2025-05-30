import type { JsonNode } from '@/api/__generated__'
import type { RJSFSchema } from '@rjsf/utils'

import i18n from '@/config/i18n.config.ts'

export const formatTagDataPoint = (data?: JsonNode) => {
  if (data) return JSON.stringify(data, null, 4)
  return i18n.t('device.drawer.tagList.formatter.unknownFormat')
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
    'mqttUserProperties'
  )
  if (!Object.keys(sourceProperties).length) throw new Error(i18n.t('device.errors.noFormSchema'))

  return {
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    definitions: {
      DeviceDataPoint: {
        type: 'object',
        title: 'Tag Address',
        description: `The address of the data-point on the device.`,
        properties: sourceProperties,
      },
      DomainTag: {
        type: 'string',
        title: 'Tag Name',
        description: `The Tag associated with the data-point.`,
        format: 'mqtt-tag',
      },
    },
    properties: {
      items: {
        type: 'array',
        title: 'List of tags',
        description: 'The list of all tags defined in the device',
        items: {
          description: `A tag associated with a data point on a device connected to the adapter`,
          required: ['tag'],
          properties: {
            tag: {
              $ref: '#/definitions/DomainTag',
            },
            dataPoint: {
              $ref: '#/definitions/DeviceDataPoint',
            },
          },
        },
      },
    },
  } as RJSFSchema
}
