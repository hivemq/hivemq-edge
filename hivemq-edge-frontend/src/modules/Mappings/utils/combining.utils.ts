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
  if (!formContext) return []

  // Use new entityQueries structure
  if (formContext.entityQueries) {
    return formContext.entityQueries.reduce<DataReference[]>((acc, entityQuery) => {
      const { entity, query } = entityQuery
      const items = query.data?.items || []
      if (!items.length) return acc

      const firstItem = items[0]

      if ((firstItem as DomainTag).name) {
        // For tags, use entity.id as scope (only for ADAPTER/EDGE_BROKER types)
        const scope = entity.type === EntityType.ADAPTER || entity.type === EntityType.EDGE_BROKER ? entity.id : null
        const tagDataReferences = (items as DomainTag[]).map<DataReference>((tag) => ({
          id: tag.name,
          type: DataIdentifierReference.type.TAG,
          scope,
        }))
        acc.push(...tagDataReferences)
      } else if ((firstItem as TopicFilter).topicFilter) {
        const topicFilterDataReferences = (items as TopicFilter[]).map<DataReference>((topicFilter) => ({
          id: topicFilter.topicFilter,
          type: DataIdentifierReference.type.TOPIC_FILTER,
          scope: null,
        }))
        acc.push(...topicFilterDataReferences)
      }

      return acc
    }, [])
  }

  // Backward compatibility: fall back to old structure during migration
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
        scope: null,
      }))
      acc.push(...topicFilterDataReferences)
    }

    return acc
  }, [])
}

export const getFilteredDataReferences = (formData?: DataCombining, formContext?: CombinerContext) => {
  // Use selectedSources from context if available (Phase 2+)
  // This provides full ownership information without reconstruction
  if (formContext?.selectedSources) {
    const { tags, topicFilters } = formContext.selectedSources
    const allReferences = [...tags, ...topicFilters]

    // Deduplicate by id + type + scope
    // This allows tags with same name from different adapters to load separate schemas
    return allReferences.reduce<DataReference[]>((acc, current) => {
      const isAlreadyIn = acc.find(
        (item) => item.id === current.id && item.type === current.type && item.scope === current.scope
      )
      if (!isAlreadyIn) {
        return acc.concat([current])
      }
      return acc
    }, [])
  }

  // Fallback to old behavior (Phase 1, backward compatibility)
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
 * @param formContext - The combiner context with entityQueries
 * @returns The adapterId (scope) or undefined if not found
 */
export const getAdapterIdForTag = (tagId: string, formContext?: CombinerContext): string | undefined => {
  if (!formContext) return undefined

  // Use new entityQueries structure
  if (formContext.entityQueries) {
    for (const { entity, query } of formContext.entityQueries) {
      if (entity.type !== EntityType.ADAPTER) continue

      const items = query.data?.items || []
      if (items.length > 0 && (items[0] as DomainTag).name) {
        const tags = items as DomainTag[]
        const found = tags.find((tag) => tag.name === tagId)
        if (found) {
          return entity.id // ✅ Direct access, no index needed
        }
      }
    }
    return undefined
  }

  // Backward compatibility: fall back to old structure during migration
  if (!formContext.queries || !formContext.entities) return undefined

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

/**
 * Reconstructs selectedSources from existing combiner data.
 * Used when loading an existing combiner to rebuild ownership information.
 *
 * Strategy:
 * 1. Try to find scope from primary (if it matches)
 * 2. Try to find scope from instructions
 * 3. Fallback to context lookup
 *
 * @param formData - The combiner data
 * @param formContext - The combiner context with entityQueries
 * @returns Selected sources with full ownership information
 */
export const reconstructSelectedSources = (
  formData?: DataCombining,
  formContext?: CombinerContext
): { tags: DataIdentifierReference[]; topicFilters: DataIdentifierReference[] } => {
  if (!formData?.sources) {
    return { tags: [], topicFilters: [] }
  }

  // Reconstruct tags with scope
  const tags =
    formData.sources.tags?.map((tagId) => {
      // Strategy 1: Check if this is the primary
      if (
        formData.sources.primary?.id === tagId &&
        formData.sources.primary?.type === DataIdentifierReference.type.TAG &&
        formData.sources.primary?.scope
      ) {
        return formData.sources.primary
      }

      // Strategy 2: Find scope from instructions
      const instruction = formData.instructions?.find(
        (inst) => inst.sourceRef?.id === tagId && inst.sourceRef?.type === DataIdentifierReference.type.TAG
      )
      if (instruction?.sourceRef?.scope) {
        return instruction.sourceRef
      }

      // Strategy 3: Fallback to context lookup
      const adapterId = getAdapterIdForTag(tagId, formContext)
      return {
        id: tagId,
        type: DataIdentifierReference.type.TAG,
        scope: adapterId ?? null,
      }
    }) || []

  // Reconstruct topic filters (scope always null)
  const topicFilters =
    formData.sources.topicFilters?.map((tfId) => {
      // Check if this is the primary (to preserve exact reference)
      if (
        formData.sources.primary?.id === tfId &&
        formData.sources.primary?.type === DataIdentifierReference.type.TOPIC_FILTER
      ) {
        return formData.sources.primary
      }

      // Check instructions
      const instruction = formData.instructions?.find(
        (inst) => inst.sourceRef?.id === tfId && inst.sourceRef?.type === DataIdentifierReference.type.TOPIC_FILTER
      )
      if (instruction?.sourceRef) {
        return instruction.sourceRef
      }

      // Default topic filter
      return {
        id: tfId,
        type: DataIdentifierReference.type.TOPIC_FILTER,
        scope: null,
      }
    }) || []

  return { tags, topicFilters }
}
