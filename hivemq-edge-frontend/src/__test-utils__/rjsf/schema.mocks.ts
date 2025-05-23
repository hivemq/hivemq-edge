import type { RJSFSchema } from '@rjsf/utils'

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
    nestedObject: {
      type: 'object',
      title: 'NestedObject',
      properties: {
        name: {
          type: 'string',
          default: 'Default name',
        },
        age: {
          type: 'integer',
          title: 'Age',
        },
      },
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

export const MOCK_MQTT_SCHEMA_METADATA: RJSFSchema = {
  title: 'The name of the schema',
  description: 'The description of the schema',
  type: 'object',
  required: ['string', 'number'],
  properties: {
    string: {
      type: 'string',
      title: 'string',
      description: 'The description for a string property',
      maxLength: 256,
      minLength: 1,
      pattern: '^(\\([0-9]{3}\\))?[0-9]{3}-[0-9]{4}$',
      examples: ['12345', 'abcdef'],
    },
    number: {
      type: 'number',
      title: 'number',
      description: 'The description for a number property',
      multipleOf: 2,
      maximum: 150,
      exclusiveMaximum: 148,
      minimum: 2,
      exclusiveMinimum: 4,
      examples: [1, 2, 3, 4],
    },
    integer: {
      type: 'integer',
      title: 'integer',
      description: 'The description for a integer property',
      multipleOf: 2,
      maximum: 150,
      exclusiveMaximum: 148,
      minimum: 2,
      exclusiveMinimum: 4,
      examples: [1, 2, 3, 4],
    },
    boolean: {
      type: 'boolean',
      title: 'boolean',
      description: 'The description for a boolean property',
      examples: [true],
    },
    array: {
      type: 'array',
      title: 'array',
      description: 'The description for a array property',
      examples: [[1, 2, 3]],
      maxItems: 10,
      minItems: 2,
      uniqueItems: true,
      items: {
        type: 'integer',
        title: 'integer',
        description: 'The description for a integer property',
        multipleOf: 2,
        maximum: 150,
        exclusiveMaximum: 148,
        minimum: 2,
        exclusiveMinimum: 4,
        examples: [1, 2, 3, 4],
      },
      // contains is not yet supported
    },
    object: {
      type: 'object',
      title: 'object',
      description: 'The description for a object property',
      examples: [{ test: 1 }],
      maxProperties: 4,
      minProperties: 1,
    },
  },
}

export const MOCK_EMPTY_SCHEMA_URI =
  'data:application/json;base64,ewogICIkc2NoZW1hIjogImh0dHA6Ly9qc29uLXNjaGVtYS5vcmcvZHJhZnQtMDcvc2NoZW1hIyIsCiAgInRpdGxlIjogIkN1c3RvbVN0cnVjdDogbnM9MztzPVRFX1wiVXNlcl9kYXRhX3R5cGVfNlwiIiwKICAidHlwZSI6ICJvYmplY3QiLAogICJwcm9wZXJ0aWVzIjogewogIH0sCiAgInJlcXVpcmVkIjogWyJ2YWx1ZSJdCn0K'

export const MOCK_SIMPLE_SCHEMA_URI =
  'data:application/json;base64,ewogICIkc2NoZW1hIjogImh0dHA6Ly9qc29uLXNjaGVtYS5vcmcvZHJhZnQtMDcvc2NoZW1hIyIsCiAgInRpdGxlIjogIkN1c3RvbVN0cnVjdDogbnM9MztzPVRFX1wiVXNlcl9kYXRhX3R5cGVfNlwiIiwKICAidHlwZSI6ICJvYmplY3QiLAogICJwcm9wZXJ0aWVzIjogewogICAgInZhbHVlIjogewogICAgICAidHlwZSI6ICJzdHJpbmciCiAgICB9CiAgfSwKICAicmVxdWlyZWQiOiBbInZhbHVlIl0KfQo='
