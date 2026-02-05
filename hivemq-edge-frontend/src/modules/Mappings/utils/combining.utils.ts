import type { UseQueryResult } from '@tanstack/react-query'

import {
  DataIdentifierReference,
  type DataCombining,
  type DomainTag,
  type EntityReference,
  type JsonNode,
  type TopicFilter,
  EntityType,
} from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { AUTO_MATCH_DISTANCE } from '@/components/rjsf/BatchModeMappings/utils/config.utils'
import levenshtein from '@/components/rjsf/BatchModeMappings/utils/levenshtein.utils'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
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
  const dataSources = entities.filter((e) => e.type === EntityType.ADAPTER || e.type === EntityType.EDGE_BROKER)
  return content.reduce<DataReference[]>((acc, cur, currentIndex) => {
    const firstItem = cur[0]
    if (!firstItem) return acc

    if ((firstItem as DomainTag).name) {
      // This is a domain tag
      const tagDataReferences = (cur as DomainTag[]).map<DataReference>((tag) => {
        return {
          id: tag.name,
          type: DataIdentifierReference.type.TAG,
          scope: dataSources?.[currentIndex]?.id,
        }
      })
      acc.push(...tagDataReferences)
    } else if ((firstItem as TopicFilter).topicFilter) {
      // This is a topic filter
      const topicFilterDataReferences = (cur as TopicFilter[]).map<DataReference>((topicFilter) => ({
        id: topicFilter.topicFilter,
        type: DataIdentifierReference.type.TOPIC_FILTER,
        scope: null, // ✅ Topic filters always have null scope
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
      dataReference.schema = validateSchemaFromDataURI(data, dataReference.type)
    } else if (typeof data === 'object') {
      dataReference.schema = {
        schema: data || undefined,
        status: 'success',
        message: i18n.t('schema.status.success', { context: dataReference.type }),
      }
    } else {
      dataReference.schema = {
        status: 'warning',
        message: i18n.t('schema.validation.noAssignedSchema', { context: dataReference.type }),
      }
    }
    return dataReference
  })
}

export type AutoMatchAccumulator = {
  distance: number
  value: FlatJSONSchema7
}

/**
 *
 * @param source
 * @param candidates
 * @param minDistance The minimum Levenshtein distance for a match, `null` for the best candidate
 */
export const findBestMatch = (
  source: FlatJSONSchema7,
  candidates: FlatJSONSchema7[],
  minDistance: number | null = AUTO_MATCH_DISTANCE
): AutoMatchAccumulator | undefined => {
  const smallestValue = candidates.reduce<AutoMatchAccumulator>((acc, property) => {
    const fullPath = (property: FlatJSONSchema7) => [...property.path, property.key].join('.')

    if (source.type !== property.type) return acc

    const distance = Math.min(
      ...[levenshtein(property.key, source.key), levenshtein(fullPath(property), fullPath(source))]
    )
    return distance < acc.distance || acc.distance === undefined ? { value: property, distance } : acc
  }, {} as AutoMatchAccumulator)

  if (minDistance === null) return smallestValue
  return smallestValue.distance <= minDistance ? smallestValue : undefined
}

/**
 * Extracts the adapterId (scope) for a given tag from context.
 * Used for looking up scope when only tag name is available.
 *
 * @param tagId - The tag identifier
 * @param formContext - The combiner context with queries and entities
 * @returns The adapterId (scope) or undefined if not found
 */
export const getAdapterIdForTag = (tagId: string, formContext?: CombinerContext): string | undefined => {
  if (!formContext?.queries || !formContext?.entities) return undefined

  const adapterEntities = formContext.entities.filter((e) => e.type === EntityType.ADAPTER)

  for (let i = 0; i < formContext.queries.length; i++) {
    const query = formContext.queries[i]
    const items = query.data?.items || []

    if (items.length > 0 && (items[0] as DomainTag).name) {
      const tags = items as DomainTag[]
      const found = tags.find((tag) => tag.name === tagId)
      if (found && adapterEntities[i]) {
        return adapterEntities[i].id
      }
    }
  }

  return undefined
}
