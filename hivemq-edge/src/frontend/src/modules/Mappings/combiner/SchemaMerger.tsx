import { type FC, useMemo } from 'react'
import { Box, List, ListItem, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'

import type { CombinerContext } from '../types'
import { getFilteredDataReferences, getSchemasFromReferences } from '../utils/combining.utils'

interface SchemaMergerProps {
  formData?: DataCombining
  formContext?: CombinerContext
  onUpload: (properties: FlatJSONSchema7[]) => void
}

const SchemaMerger: FC<SchemaMergerProps> = ({ formData, formContext }) => {
  const { t } = useTranslation()

  const references = useMemo(() => {
    return getFilteredDataReferences(formData, formContext)
  }, [formContext, formData])

  const schemaQueries = useGetCombinedDataSchemas(references)

  const properties = useMemo(() => {
    const displayedSchemas = getSchemasFromReferences(references, schemaQueries)

    return displayedSchemas.reduce<FlatJSONSchema7[]>((acc, cur) => {
      if (!cur.schema?.schema) return acc
      const props = getPropertyListFrom(cur.schema.schema)
      props.forEach((e) => {
        if (cur.type === DataIdentifierReference.type.TAG) {
          const index = formData?.sources?.tags?.findIndex((tag) => tag === cur.id)
          e.origin = `tg${index}`
        } else if (cur.type === DataIdentifierReference.type.TOPIC_FILTER) {
          const index = formData?.sources?.topicFilters?.findIndex((tag) => tag === cur.id)
          e.origin = `tf${index}`
        }
        if (e.path.length === 0) {
          e.key = `${e.origin}_${e.title}`
          e.title = e.key
        }
      })
      acc.push(...props)
      return acc
    }, [])
  }, [formData?.sources?.tags, formData?.sources?.topicFilters, references, schemaQueries])

  return (
    <VStack spacing={2} alignItems={'flex-start'}>
      <Text>{t('combiner.schema.schemaManager.infer.message')}</Text>

      <Box>
        <List>
          {properties.map((property) => {
            return (
              <ListItem key={[...property.path, property.key].join('-')} ml={(property?.path?.length || 0) * 8}>
                <PropertyItem property={property} hasTooltip />
              </ListItem>
            )
          })}
        </List>
      </Box>
    </VStack>
  )
}

export default SchemaMerger
