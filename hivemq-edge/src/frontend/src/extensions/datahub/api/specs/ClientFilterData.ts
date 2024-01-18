import { PanelSpecs } from '@/extensions/datahub/types.ts'

export const MOCK_CLIENT_FILTER_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    description: `To start a behavior policy, you have to indicate a client (or a group of client) to apply it on.
          `,
    properties: {
      clients: {
        type: 'array',
        title: 'Client Filters',
        description: 'Create handles on the workflow for specific clients you want to add a policy to.',
        items: {
          type: 'string',
          description: 'The client id to use as a starting point',
        },
      },
    },
  },
  uiSchema: {
    clients: {
      'ui:options': {
        orderable: false,
      },
    },
  },
}
