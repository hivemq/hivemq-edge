import { type FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Heading } from '@chakra-ui/react'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import ErrorMessage from '@/components/ErrorMessage'
import { PLCTag, TopicFilter } from '@/components/MQTT/EntityTag'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import type { CombinerContext } from '@/modules/Mappings/types'
import { getFilteredDataReferences, getSchemasFromReferences } from '@/modules/Mappings/utils/combining.utils'

interface CombinedSchemaLoaderProps {
  formData?: DataCombining
  formContext?: CombinerContext
}

export const CombinedSchemaLoader: FC<CombinedSchemaLoaderProps> = ({ formData, formContext }) => {
  const { t } = useTranslation()

  // TODO[NVL] This is almost a duplicate of the CombinedEntitySelect; reuse
  const references = useMemo(() => {
    return getFilteredDataReferences(formData, formContext)
  }, [formContext, formData])

  const schemaQueries = useGetCombinedDataSchemas(references)

  const displayedSchemas = useMemo(() => {
    return getSchemasFromReferences(references, schemaQueries)
  }, [references, schemaQueries])

  return (
    <Box borderWidth={1} p={3}>
      {!displayedSchemas.length && <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status="info" />}
      {displayedSchemas.map((dataReference) => {
        const hasSchema = dataReference.schema?.status === 'success' && dataReference.schema.schema

        if (!hasSchema) {
          // TODO[NVL] Duplication; integrate error message into the schema browser
          return (
            <Box key={dataReference.id}>
              <Heading as="h3" size="sm">
                {dataReference.type === DataIdentifierReference.type.TAG && (
                  <PLCTag tagTitle={dataReference?.id} mr={3} />
                )}
                {dataReference.type === DataIdentifierReference.type.TOPIC_FILTER && (
                  <TopicFilter tagTitle={dataReference?.id} mr={3} />
                )}
              </Heading>
              <ErrorMessage
                message={dataReference.schema?.message}
                status={dataReference.schema?.status}
                variant="light"
              />
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
            isTagShown
            px={4}
            py={3}
          />
        )
      })}
    </Box>
  )
}
