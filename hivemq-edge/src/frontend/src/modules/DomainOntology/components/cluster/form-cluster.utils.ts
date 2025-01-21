import { RJSFSchema, UiSchema } from '@rjsf/utils'
import { groupCatalog } from '@/modules/DomainOntology/utils/cluster.utils.ts'
import { CompactSelectWidget } from '@/modules/DomainOntology/components/cluster/CompactSelectWidget.tsx'

export const schema: RJSFSchema = {
  properties: {
    groups: {
      type: 'array',
      title: 'Grouping rules',
      description: 'Group adapters, bridges and devices using the following rules:',
      items: {
        type: 'string',
        enum: groupCatalog.map((e) => e.name),
      },
    },
  },
}

export const uiSchema: UiSchema = {
  groups: {
    'ui:title': null,
    items: {
      'ui:widget': CompactSelectWidget,
      'ui:addButton': 'Add a mapping',
    },
  },
}
