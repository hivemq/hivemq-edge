/* istanbul ignore file -- @preserve */
import type { RJSFSchema, UiSchema } from '@rjsf/utils'
import { groupCatalog } from '@/modules/DomainOntology/utils/cluster.utils.ts'
import { CompactSelectWidget } from '@/modules/DomainOntology/components/cluster/CompactSelectWidget.tsx'

import i18n from '@/config/i18n.config.ts'

export const schema: RJSFSchema = {
  properties: {
    groups: {
      type: 'array',
      title: i18n.t('ontology.charts.cluster.configuration.title'),
      description: i18n.t('ontology.charts.cluster.configuration.description'),
      items: {
        type: 'string',
        enum: groupCatalog.map((e) => e.name),
      },
    },
  },
}

export const uiSchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
  groups: {
    items: {
      'ui:widget': CompactSelectWidget,
      'ui:addButton': i18n.t('ontology.charts.cluster.configuration.cta.add'),
    },
  },
}
