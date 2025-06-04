import type { JSONSchema7 } from 'json-schema'

/* istanbul ignore next -- @preserve */
export const tagListJsonSchema: JSONSchema7 = {
  // $schema: 'https://json-schema.org/draft/2020-12/schema',
  definitions: {
    TagSchema: {},
  },
  properties: {
    items: {
      type: 'array',
      title: 'List of tags',
      description: 'The list of all tags defined in the device',
      items: {
        description: 'The specification of a device data point',
        title: 'Tag',
        $ref: '#/definitions/TagSchema',
      },
    },
  },
}
