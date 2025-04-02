import type { UseQueryResult } from '@tanstack/react-query'

import {
  DataIdentifierReference,
  type DataCombining,
  type DomainTag,
  type EntityReference,
  type JsonNode,
  type TopicFilter,
} from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { CombinerContext } from '@/modules/Mappings/types'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'

import i18n from '@/config/i18n.config.ts'

export const STUB_TAG_PROPERTY = 'tg'
export const STUB_TOPIC_FILTER_PROPERTY = 'tf'

// TODO[NVL] wrong data structure; simplify
/* istanbul ignore next -- @preserve */
export const getDataReference = (formContext?: CombinerContext): DataReference[] => {
  const tagsAndTopicFilters =
    formContext?.queries?.reduce<(DomainTag[] | TopicFilter[])[]>((acc, cur) => {
      const firstItem = cur.data?.items?.[0]
      if (!firstItem) return acc

      acc.push(cur.data?.items || [])
      return acc
    }, []) || []
  return getCombinedDataEntityReference(tagsAndTopicFilters, formContext?.entities || [])
}

export const getCombinedDataEntityReference = (
  content: (DomainTag[] | TopicFilter[])[],
  entities: EntityReference[]
): DataReference[] => {
  return content.reduce<DataReference[]>((acc, cur, currentIndex) => {
    const firstItem = cur[0]
    if (!firstItem) return acc
    if ((firstItem as DomainTag).name) {
      const tagDataReferences = (cur as DomainTag[]).map<DataReference>((tag) => {
        return {
          id: tag.name,
          type: DataIdentifierReference.type.TAG,
          adapterId: entities?.[currentIndex]?.id,
        }
      })
      acc.push(...tagDataReferences)
    } else if ((firstItem as TopicFilter).topicFilter) {
      const topicFilterDataReferences = (cur as TopicFilter[]).map<DataReference>((topicFilter) => ({
        id: topicFilter.topicFilter,
        type: DataIdentifierReference.type.TOPIC_FILTER,
        adapterId: undefined,
      }))
      acc.push(...topicFilterDataReferences)
    }

    return acc
  }, [])
}

export const getFilteredDataReferences = (formData?: DataCombining, formContext?: CombinerContext) => {
  const tags = formData?.sources?.tags || []
  const topicFilters = formData?.sources?.topicFilters || []
  const indexes = [...tags, ...topicFilters]

  const allDataReferences = getDataReference(formContext)

  const selectedReferences = allDataReferences?.filter((dataReference) => indexes.includes(dataReference.id)) || []
  return selectedReferences.reduce<DataReference[]>((acc, current) => {
    const isAlreadyIn = acc.find((item) => item.id === current.id && item.type === current.type)
    if (!isAlreadyIn) {
      return acc.concat([current])
    }
    return acc
  }, [])
}

export const getSchemasFromReferences = (
  references: DataReference[],
  schemaQueries: UseQueryResult<string | JsonNode | undefined, Error>[]
) => {
  return references.map((dataReference, index) => {
    const { data } = schemaQueries[index]

    // TODO[30744] Type of schema inconsistent between tag and topic filter
    if (typeof data === 'string') {
      dataReference.schema = validateSchemaFromDataURI(data)
    } else if (typeof data === 'object') {
      dataReference.schema = {
        schema: data || undefined,
        status: 'success',
        message: i18n.t('topicFilter.schema.status.success'),
      }
    } else {
      dataReference.schema = {
        status: 'warning',
        message: i18n.t('topicFilter.error.schema.noAssignedSchema', { context: dataReference.type }),
      }
    }
    return dataReference
  })
}
