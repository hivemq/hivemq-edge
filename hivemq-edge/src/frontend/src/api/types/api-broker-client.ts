import { RJSFSchema, UiSchema } from '@rjsf/utils'

/**
 * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
 */
export type BrokerClient = {
  /**
   * The configuration of the client subscriptions
   */
  config?: BrokerClientConfiguration

  /**
   * The broker client id, must be unique and only contain alpha numeric characters with spaces and hyphens.
   */
  id: string
  /**
   * The type of this instance
   */
  type: 'broker-client'
}

export type BrokerClientConfiguration = {
  /**
   * The broker client id, must be unique and only contain alpha numeric characters with spaces and hyphens.
   */
  id: string
  subscription?: BrokerClientSubscription[]
}

export type BrokerClientSubscription = {
  destination: string
  maxQoS: BrokerClientSubscription.maxQoS
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BrokerClientSubscription {
  /**
   * The maxQoS for this subscription.
   */
  export enum maxQoS {
    '_0' = 0,
    '_1' = 1,
    '_2' = 2,
  }
}

export const $BrokerClient: RJSFSchema = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  properties: {
    id: {
      type: 'string',
      title: 'Identifier',
      description: 'Unique identifier for this protocol adapter',
      minLength: 1,
      maxLength: 1024,
      format: 'identifier',
      pattern: '^([a-zA-Z_0-9-_])*$',
    },
    subscriptions: {
      title: 'subscription',
      description: 'List of subscriptions for Edge clients',
      type: 'array',
      items: {
        type: 'object',
        properties: {
          destination: {
            type: 'string',
            title: 'Destination Topic',
            description: 'The topic to publish data on',
            format: 'mqtt-topic',
          },
          includeTimestamp: {
            type: 'boolean',
            title: 'Include Sample Timestamp In Publish?',
            description: 'Include the unix timestamp of the sample time in the resulting MQTT message',
            default: true,
          },
          qos: {
            type: 'integer',
            title: 'QoS',
            description: 'MQTT Quality of Service level',
            default: 0,
            minimum: 0,
            maximum: 2,
          },
        },
        required: ['destination', 'qos'],
      },
    },
  },
  required: ['id'],
}

export const $BrokerClientUiSchema: UiSchema = {}
