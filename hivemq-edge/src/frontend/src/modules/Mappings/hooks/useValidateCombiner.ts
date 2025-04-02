import { useCallback, useMemo } from 'react'
import type { CustomValidator, RJSFSchema } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'

import type {
  Combiner,
  DataCombining,
  DomainTag,
  DomainTagList,
  EntityReference,
  TopicFilter,
  TopicFilterList,
} from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'

import { type FlatJSONSchema7, getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import type { CombinerContext } from '@/modules/Mappings/types'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'

// TODO[NVL] Context is not part of the customValidator props; need to get a better construction of props
export const useValidateCombiner = (
  queries: UseQueryResult<DomainTagList | TopicFilterList, Error>[],
  entities: EntityReference[]
) => {
  const { t } = useTranslation()
  const { data: adapterInfo, isSuccess: isProtocolSuccess } = useGetAdapterTypes()
  const { data: adapters, isSuccess: isAdapterSuccess } = useListProtocolAdapters()

  const isSuccess = isProtocolSuccess && isAdapterSuccess

  const hasAdapterCapability = useCallback(
    (id: string) => {
      const adapter = adapters?.find((e) => e.id === id)
      const protocolAdapter = adapter ? adapterInfo?.items.find((protocol) => protocol.id === adapter.type) : undefined
      if (!adapter || !protocolAdapter) return undefined

      return protocolAdapter.capabilities && protocolAdapter.capabilities.includes('COMBINE')
    },
    [adapterInfo?.items, adapters]
  )

  // TODO[NVL] This is a duplicate from CombinedSchemaLoader; refactor
  const allReferences = useMemo<DataReference[]>(() => {
    return queries?.reduce<DataReference[]>((acc, cur, currentIndex) => {
      const firstItem = cur.data?.items?.[0]
      if (!firstItem) return acc
      if ((firstItem as DomainTag).name) {
        const tagDataReferences = (cur.data?.items as DomainTag[]).map<DataReference>((tag) => ({
          id: tag.name,
          type: DataIdentifierReference.type.TAG,
          adapterId: entities?.[currentIndex]?.id,
        }))
        acc.push(...tagDataReferences)
      } else if ((firstItem as TopicFilter).topicFilter) {
        const topicFilterDataReferences = (cur.data?.items as TopicFilter[]).map<DataReference>((topicFilter) => ({
          id: topicFilter.topicFilter,
          type: DataIdentifierReference.type.TOPIC_FILTER,
          adapterId: undefined,
        }))
        acc.push(...topicFilterDataReferences)
      }

      return acc
    }, [])
  }, [entities, queries])

  const allSchemaReferences = useGetCombinedDataSchemas(allReferences)

  // TODO[NVL] This is overlapping with allReferences; refactor
  const allDataSourcesFromEntities = useMemo(() => {
    return queries.reduce<{ tags: string[]; topicFilters: string[] }>(
      (acc, cur) => {
        const items = cur.data?.items
        if (!items?.[0]) return acc

        if ((items[0] as DomainTag).name) {
          acc.tags.push(...(items as DomainTag[]).map((e) => e.name))
        }
        if ((items[0] as TopicFilter).topicFilter) {
          acc.topicFilters.push(...(items as TopicFilter[]).map((e) => e.topicFilter))
        }

        return acc
      },
      { tags: [], topicFilters: [] }
    )
  }, [queries])

  const allPathsFromSources = useMemo<string[]>(() => {
    return allSchemaReferences.reduce<string[]>((acc, cur) => {
      let flatList: FlatJSONSchema7[]
      if (typeof cur.data === 'string') {
        const handleSchema = validateSchemaFromDataURI(cur.data)
        flatList = handleSchema.schema ? getPropertyListFrom(handleSchema.schema) : []
      } else {
        flatList = cur.data ? getPropertyListFrom(cur.data) : []
      }
      const fullPaths = flatList.map((e) => [...e.path, e.key].join('.'))
      acc.push(...fullPaths)

      return Array.from(new Set(acc))
    }, [])
  }, [allSchemaReferences])

  /**
   * Verify that a connected entity is eligible for data combining:
   * - ADAPTER: must have the `COMBINE` capability
   * - BRIDGE: no condition
   *     TODO[NVL] Should we ban bridges that have no proper topic filters
   * - EDGE: mandatory inclusion to the sources
   */
  const validateSourceCapability = useCallback<CustomValidator<Combiner, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      formData?.sources.items.forEach((entity, index) => {
        if (entity.type === EntityType.ADAPTER) {
          const isCapable = hasAdapterCapability(entity.id)
          if (isCapable === undefined)
            errors.sources?.items?.[index]?.addError(t('combiner.error.validation.notValidReference'))
          else if (!isCapable)
            errors.sources?.items?.[index]?.addError(t('combiner.error.validation.notCombineCapability'))
        }
      })

      const hasEdge = formData?.sources.items?.filter((e) => e.type === EntityType.EDGE_BROKER)
      if (!hasEdge || hasEdge.length !== 1) {
        errors.sources?.items?.addError(t('combiner.error.validation.notEdgeSource'))
      }

      return errors
    },
    [hasAdapterCapability, t]
  )

  /**
   * Verify that a data source (tag or topic filter) belongs to a connected entity
   */
  const validateDataSources = useCallback<CustomValidator<DataCombining, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      formData?.sources?.tags?.map((tag) => {
        if (!allDataSourcesFromEntities.tags.includes(tag)) {
          errors.sources?.tags?.addError(
            t('combiner.error.validation.notDataSourceOwner', {
              context: DataIdentifierReference.type.TAG,
              tag,
            })
          )
        }
      })
      formData?.sources?.topicFilters?.map((topicFilter) => {
        if (!allDataSourcesFromEntities.topicFilters.includes(topicFilter)) {
          errors.sources?.tags?.addError(
            t('combiner.error.validation.notDataSourceOwner', {
              context: DataIdentifierReference.type.TOPIC_FILTER,
              topicFilter,
            })
          )
        }
      })

      return errors
    },
    [allDataSourcesFromEntities.tags, allDataSourcesFromEntities.topicFilters, t]
  )

  /**
   * Verify that the schema of a destination topic is valid
   */
  const validateDestinationSchema = useCallback<CustomValidator<DataCombining, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      const handleSchema = validateSchemaFromDataURI(formData?.destination?.schema)
      if (handleSchema.status !== 'success' || !handleSchema.schema)
        errors.destination?.schema?.addError(handleSchema.error || handleSchema.message)
      else {
        const properties = getPropertyListFrom(handleSchema.schema)
        if (!properties.length)
          errors.destination?.schema?.addError(t('combiner.error.validation.notDestinationProperties'))
      }
      return errors
    },
    [t]
  )

  /**
   * Verify that every schema of the data sources is valid or that there is at least one valid schema
   */
  const validateDataSourceSchemas = useCallback<CustomValidator<DataCombining, RJSFSchema, CombinerContext>>(
    (_, errors) => {
      const hasAtLeastOneSchema = allSchemaReferences.some((e) => e.data !== undefined && e.isSuccess)
      if (!hasAtLeastOneSchema) {
        errors.sources?.addError(t('combiner.error.validation.notMinimumRequiredSchema'))
      }

      // TODO[NVL] These validation are wrong; need to refactor the data structure for clarity
      // const nbTag = formData?.sources.tags?.length || 0
      // allSchemaReferences.forEach((query, index) => {
      //   if (typeof query.data === 'string') {
      //     const handleSchema = validateSchemaFromDataURI(query.data)
      //     if (handleSchema.status !== 'success' || !handleSchema.schema) {
      //       if (index > nbTag) errors.sources?.topicFilters?.addError(handleSchema.error || handleSchema.message)
      //       else errors.sources?.tags?.addError(handleSchema.error || handleSchema.message)
      //     } else {
      //       const properties = getPropertyListFrom(handleSchema.schema)
      //       if (!properties.length) {
      //         if (index > nbTag)
      //           errors.sources?.topicFilters?.addError(
      //             t('combiner.error.validation.notDataSourceProperties', {
      //               context: DataIdentifierReference.type.TOPIC_FILTER,
      //             })
      //           )
      //         else
      //           errors.sources?.tags?.addError(
      //             t('combiner.error.validation.notDataSourceProperties', {
      //               context: DataIdentifierReference.type.TAG,
      //             })
      //           )
      //       }
      //     }
      //   }
      // })

      return errors
    },
    [allSchemaReferences, t]
  )

  const validateInstructions = useCallback<CustomValidator<DataCombining, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      const handleSchema = validateSchemaFromDataURI(formData?.destination?.schema)
      const properties = handleSchema.schema && getPropertyListFrom(handleSchema.schema)

      const knownPaths = properties?.map((e) => [...e.path, e.key].join('.')) || []

      // TODO[NVL] validation is required; need to refactor the data structure for clarity
      // if (!formData?.instructions || formData?.instructions.length === 0)
      //   errors.addError(t('combiner.error.validation.notInstruction'))

      formData?.instructions?.forEach((instruction, index) => {
        if (!knownPaths.includes(instruction.destination))
          errors.instructions?.[index]?.addError(t('combiner.error.validation.notInstructionDestinationPath'))

        if (!allPathsFromSources.includes(instruction.source))
          errors.instructions?.[index]?.addError(t('combiner.error.validation.notInstructionSourcePath'))
      })

      return errors
    },
    [allPathsFromSources, t]
  )

  const validateCombiner: CustomValidator<Combiner, RJSFSchema, CombinerContext> = (formData, errors) => {
    if (!formData) {
      errors.addError(t('combiner.error.validation.notValidPayload'))
      return errors
    }

    validateSourceCapability(formData, errors)

    formData?.mappings?.items?.forEach((entity, index) => {
      if (!errors.mappings?.items?.[index]) return

      validateDataSources(entity, errors.mappings.items[index])
      validateDataSourceSchemas(entity, errors.mappings.items[index])
      validateDestinationSchema(entity, errors.mappings.items[index])
      validateInstructions(entity, errors.mappings.items[index])
    })

    return errors
  }

  return isSuccess ? validateCombiner : undefined
}
