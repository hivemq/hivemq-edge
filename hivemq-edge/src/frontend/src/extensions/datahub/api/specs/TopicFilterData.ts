import { PanelSpecs } from '@/extensions/datahub/types.ts'

export const MOCK_TOPIC_FILTER_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    description: `To start a policy, you have to indicate a topic (or a topic filter) to apply it on. This
         is a simple way to handle this operation, until we can use the Topic Tree Selector.
          `,
    properties: {
      topics: {
        type: 'array',
        title: 'Client Filters',
        description: 'Create handles on the workflow for specific topics or topic filters you want to add a policy to.',
        items: {
          type: 'string',
          description: 'The full topic to use as a starting point',
        },
      },
    },
  },
  uiSchema: {
    topics: {
      'ui:options': {
        orderable: false,
      },
    },
  },
}
