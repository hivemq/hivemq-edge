import { type FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Heading } from '@chakra-ui/react'

import type { DataCombining, DomainTag, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import ErrorMessage from '@/components/ErrorMessage'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import type { CombinerContext } from '@/modules/Mappings/types'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'

interface CombinedSchemaLoaderProps {
  formData?: DataCombining
  formContext?: CombinerContext
}

export const CombinedSchemaLoader: FC<CombinedSchemaLoaderProps> = ({ formData, formContext }) => {
  const { t } = useTranslation()

  // TODO[NVL] This is almost a duplicate of the CombinedEntitySelect; reuse
  const allDataReferences = useMemo(() => {
    return formContext?.queries?.reduce<DataReference[]>((acc, cur) => {
      const firstItem = cur.data?.items?.[0]
      if (!firstItem) return acc
      if ((firstItem as DomainTag).name) {
        const tagDataReferences = (cur.data?.items as DomainTag[]).map<DataReference>((tag, index) => ({
          id: tag.name,
          type: DataIdentifierReference.type.TAG,
          adapterId: formContext.entities?.[index]?.id,
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
  }, [formContext?.entities, formContext?.queries])

  const allSchemas = useMemo(() => {
    const tags = formData?.sources?.tags || []
    const topicFilters = formData?.sources?.topicFilters || []
    const indexes = [...tags, ...topicFilters]

    const selectedReferences = allDataReferences?.filter((dataReference) => indexes.includes(dataReference.id)) || []
    return selectedReferences.reduce<DataReference[]>((acc, current) => {
      const isAlreadyIn = acc.find((item) => item.id === current.id && item.type === current.type)
      if (!isAlreadyIn) {
        return acc.concat([current])
      }
      return acc
    }, [])
  }, [allDataReferences, formData?.sources?.tags, formData?.sources?.topicFilters])

  const queries = useGetCombinedDataSchemas(allSchemas)

  const displayedSchemas = useMemo(() => {
    return allSchemas.map((dataReference, index) => {
      const { data } = queries[index]

      // TODO[30744] Type of schema inconsistent between tag and topic filter
      if (typeof data === 'string') {
        dataReference.schema = validateSchemaFromDataURI(data)
      } else if (typeof data === 'object') {
        dataReference.schema = {
          schema: data,
          status: 'success',
          message: t('topicFilter.schema.status.success'),
        }
      } else {
        dataReference.schema = {
          status: 'warning',
          message: t('topicFilter.schema.status.missing'),
        }
      }
      return dataReference
    })
  }, [allSchemas, queries, t])

  if (!displayedSchemas.length) return <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status={'info'} />

  return (
    <>
      {displayedSchemas.map((dataReference) => {
        const hasSchema = dataReference.schema?.status === 'success' && dataReference.schema.schema

        if (!hasSchema) {
          // TODO[NVL] Duplication; integrate error message into the schema browser
          return (
            <Box key={dataReference.id}>
              <Heading as="h4" size="sm">
                {dataReference.id}
              </Heading>
              <ErrorMessage message={dataReference.schema?.message} status={dataReference.schema?.status} />
            </Box>
          )
        }
        return (
          <JsonSchemaBrowser
            dataReference={dataReference}
            key={dataReference.id}
            schema={{ ...dataReference.schema?.schema, title: dataReference.id }}
            isDraggable
            hasExamples
          />
        )
      })}
    </>
  )
}
