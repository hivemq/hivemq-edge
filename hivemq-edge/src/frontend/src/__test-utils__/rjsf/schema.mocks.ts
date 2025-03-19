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

export const MOCK_EMPTY_SCHEMA_URI =
  'data:application/json;base64,ewogICIkc2NoZW1hIjogImh0dHA6Ly9qc29uLXNjaGVtYS5vcmcvZHJhZnQtMDcvc2NoZW1hIyIsCiAgInRpdGxlIjogIkN1c3RvbVN0cnVjdDogbnM9MztzPVRFX1wiVXNlcl9kYXRhX3R5cGVfNlwiIiwKICAidHlwZSI6ICJvYmplY3QiLAogICJwcm9wZXJ0aWVzIjogewogIH0sCiAgInJlcXVpcmVkIjogWyJ2YWx1ZSJdCn0K'

export const MOCK_SIMPLE_SCHEMA_URI =
  'data:application/json;base64,ewogICIkc2NoZW1hIjogImh0dHA6Ly9qc29uLXNjaGVtYS5vcmcvZHJhZnQtMDcvc2NoZW1hIyIsCiAgInRpdGxlIjogIkN1c3RvbVN0cnVjdDogbnM9MztzPVRFX1wiVXNlcl9kYXRhX3R5cGVfNlwiIiwKICAidHlwZSI6ICJvYmplY3QiLAogICJwcm9wZXJ0aWVzIjogewogICAgInZhbHVlIjogewogICAgICAidHlwZSI6ICJzdHJpbmciCiAgICB9CiAgfSwKICAicmVxdWlyZWQiOiBbInZhbHVlIl0KfQo='
