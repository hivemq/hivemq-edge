import { RJSFSchema } from '@rjsf/utils'
import { faker } from '@faker-js/faker'
import { DeviceTags } from '@/modules/Subscriptions/types.ts'

export const GENERATE_DATA_MODELS = (_count?: number, short = false): RJSFSchema => {
  const model: RJSFSchema = {
    title: 'A registration form',
    description: 'A simple form example.',
    type: 'object',
    required: ['firstName', 'lastName'],
    properties: {
      firstName: {
        type: 'string',
        title: faker.lorem.word(),
        default: 'Chuck',
      },
      lastName: {
        type: 'string',
        title: faker.lorem.word(),
      },
      age: {
        type: 'integer',
        title: faker.lorem.word(),
      },
    },
  }

  if (!short)
    model.properties = {
      ...model.properties,
      bio: {
        type: 'string',
        title: faker.lorem.word(),
      },
      password: {
        type: 'string',
        title: faker.lorem.word(),
        minLength: 3,
      },
      telephone: {
        type: 'string',
        title: faker.lorem.word(),
        minLength: 10,
      },
      listOfStrings: {
        type: 'array',
        title: faker.lorem.word(),
        items: {
          type: 'string',
          default: 'bazinga',
        },
      },
      minItemsList: {
        type: 'array',
        title: faker.lorem.word(),
        minItems: 3,
        items: {
          type: 'object',
          properties: {
            name: {
              type: 'string',
              title: faker.lorem.word(),
              default: 'Default name',
            },
          },
        },
      },
    }

  return model
}

export const MOCK_MQTT_SCHEMA_REFS: RJSFSchema = {
  definitions: {
    address: {
      type: 'object',
      properties: {
        street_address: {
          type: 'string',
        },
        city: {
          type: 'string',
        },
        state: {
          type: 'string',
        },
      },
      required: ['street_address', 'city', 'state'],
    },
    node: {
      type: 'object',
      properties: {
        name: {
          type: 'string',
        },
        children: {
          type: 'array',
          items: {
            $ref: '#/definitions/node',
          },
        },
      },
    },
  },
  type: 'object',
  properties: {
    billing_address: {
      title: 'Billing address',
      $ref: '#/definitions/address',
    },
    shipping_address: {
      title: 'Shipping address',
      $ref: '#/definitions/address',
    },
    tree: {
      title: 'Recursive references',
      $ref: '#/definitions/node',
    },
  },
}

export const MOCK_MQTT_SCHEMA_PLAIN: RJSFSchema = {
  title: 'A registration form',
  description: 'A simple form example.',
  type: 'object',
  required: ['firstName', 'lastName'],
  properties: {
    firstName: {
      type: 'string',
      title: 'First name',
      default: 'Chuck',
    },
    lastName: {
      type: 'string',
      title: 'Last name',
    },
    age: {
      type: 'integer',
      title: 'Age',
    },
    bio: {
      type: 'string',
      title: 'Bio',
    },
    password: {
      type: 'string',
      title: 'Password',
      minLength: 3,
    },
    telephone: {
      type: 'string',
      title: 'Telephone',
      minLength: 10,
    },
    listOfStrings: {
      type: 'array',
      title: 'A list of strings',
      items: {
        type: 'string',
        default: 'bazinga',
      },
    },
    minItemsList: {
      type: 'array',
      title: 'A list with a minimal number of items',
      minItems: 3,
      items: {
        type: 'object',
        properties: {
          name: {
            type: 'string',
            default: 'Default name',
          },
        },
      },
    },
  },
}
export const MOCK_MQTT_TOPIC_SAMPLES = [
  'tmp/broker1/topic1/segment1',
  'tmp/broker1/topic1/segment2',
  'tmp/broker1/topic1/segment2/leaf1',
  'tmp/broker2/topic1',
  'tmp/broker4/topic1/segment2',
]

export const MOCK_DEVICE_TAGS: DeviceTags[] = [
  { tag: 'write/power-management/alert', node: 'ns=3;i=1002' },
  { tag: 'write/power-management/off', node: 'ns=3;i=1003' },
  { tag: 'write/log/event', node: 'ns=3;i=1008' },
]
