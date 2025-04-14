import { type FC, useMemo } from 'react'
import {
  Box,
  Button,
  ButtonGroup,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  List,
  ListItem,
  ModalBody,
  ModalFooter,
  Text,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem'
import { type FlatJSONSchema7, getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'

import type { CombinerContext } from '../types'
import {
  getFilteredDataReferences,
  getSchemasFromReferences,
  STUB_TAG_PROPERTY,
  STUB_TOPIC_FILTER_PROPERTY,
} from '../utils/combining.utils'

interface SchemaMergerProps {
  formData?: DataCombining
  formContext?: CombinerContext
  onUpload: (properties: FlatJSONSchema7[]) => void
  onClose: () => void
}

const SchemaMerger: FC<SchemaMergerProps> = ({ formData, formContext, onClose, onUpload }) => {
  const { t } = useTranslation()

  const references = useMemo(() => {
    return getFilteredDataReferences(formData, formContext)
  }, [formContext, formData])

  const schemaQueries = useGetCombinedDataSchemas(references)
  const errorMessages: string[] = []

  const properties = useMemo(() => {
    const displayedSchemas = getSchemasFromReferences(references, schemaQueries)

    const tagIndexMap = new Map(formData?.sources?.tags?.map((tag, index) => [tag, index]))
    const topicFilterIndexMap = new Map(formData?.sources?.topicFilters?.map((filter, index) => [filter, index]))

    return displayedSchemas.reduce<FlatJSONSchema7[]>((acc, reference) => {
      if (!reference.schema?.schema) return acc
      const properties = getPropertyListFrom(reference.schema.schema)

      const conflictName: [number, string][] = []
      properties.forEach((property, pathIndex) => {
        property.metadata = reference
        if (reference.type === DataIdentifierReference.type.TAG) {
          const index = tagIndexMap.get(reference.id)
          property.origin = `${STUB_TAG_PROPERTY}${index}`
        } else if (reference.type === DataIdentifierReference.type.TOPIC_FILTER) {
          const index = topicFilterIndexMap.get(reference.id)
          property.origin = `${STUB_TOPIC_FILTER_PROPERTY}${index}`
        }
        if (property.path.length === 0) {
          property.key = `${property.origin}_${property.title}`
          property.title = property.key
          conflictName.push([pathIndex, property.key])
        } else {
          conflictName.forEach(([index, newTitle]) => {
            if (property.path[index]) {
              property.path[index] = newTitle
            }
          })
        }
      })
      acc.push(...properties)
      return acc
    }, [])
  }, [formData?.sources?.tags, formData?.sources?.topicFilters, references, schemaQueries])

  if (!references.length) errorMessages.push(t('combiner.error.schemaManager.noSourceSchemaDefined'))
  else if (!properties.length) errorMessages.push(t('combiner.error.schemaManager.noValidSourceSchema'))

  const isError = Boolean(errorMessages.length)
  return (
    <>
      <ModalBody>
        <VStack spacing={4} alignItems={'flex-start'}>
          <Text data-testid={'schema-infer-prompt'}>{t('combiner.schema.schemaManager.infer.message')}</Text>

          <FormControl isInvalid={isError} data-testid={'schema-infer-merged'} id={'schema-infer-properties'}>
            <FormLabel>{t('combiner.schema.schemaManager.infer.title')}</FormLabel>
            <Box borderWidth={1} p={2} borderColor={isError ? 'red.500' : 'inherit'}>
              <List minH={50}>
                {properties.map((property) => {
                  return (
                    <ListItem key={[...property.path, property.key].join('-')} ml={(property?.path?.length || 0) * 8}>
                      <PropertyItem property={property} hasTooltip />
                    </ListItem>
                  )
                })}
              </List>
            </Box>
            {!isError && <FormHelperText>{t('combiner.schema.schemaManager.infer.helper')}</FormHelperText>}
            <FormErrorMessage>{errorMessages.join('. ')}</FormErrorMessage>
          </FormControl>
        </VStack>
      </ModalBody>
      <ModalFooter>
        <ButtonGroup variant="outline">
          <Button data-testid={'schema-infer-cancel'} onClick={onClose}>
            {t('action.cancel')}
          </Button>
          <Button
            data-testid={'schema-infer-generate'}
            isDisabled={isError}
            onClick={() => onUpload(properties)}
            variant="primary"
          >
            {t('combiner.schema.schemaManager.infer.action')}
          </Button>
        </ButtonGroup>
      </ModalFooter>
    </>
  )
}

export default SchemaMerger
