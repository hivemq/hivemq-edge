import { RJSFSchema } from '@rjsf/utils'

export const GENERATE_DATA_MODELS = (short = false, title?: string): RJSFSchema => {
  const model: RJSFSchema = {
    title: title || 'A registration form',
    description: 'A simple form example.',
    type: 'object',
    required: ['firstName', 'lastName'],
    properties: {
      firstName: {
        type: 'string',
        title: short ? 'First String' : 'firstName',
        default: 'Chuck',
        examples: 'firstName',
      },
      lastName: {
        type: 'string',
        title: short ? 'Second String' : 'lastname',
      },
      age: {
        type: 'integer',
        title: short ? 'Number' : 'age',
      },
      subItems: {
        type: 'object',
        title: 'subItems',
        examples: 'subItems',

        properties: {
          name: {
            examples: 'name',
            type: 'string',
            title: 'name',
            default: 'Default name',
          },
          type: {
            type: 'string',
            title: 'type',
            default: 'Default type',
          },
        },
      },
    },
  }

  if (!short)
    model.properties = {
      ...model.properties,
      bio: {
        type: 'string',
        title: 'bio',
      },
      password: {
        type: 'string',
        title: 'password',
        minLength: 3,
      },
      telephone: {
        type: 'string',
        title: 'telephone',
        minLength: 10,
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
