import type { FC } from 'react'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import { getTemplate, getUiOptions } from '@rjsf/utils'
import {
  Box,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  HStack,
  Icon,
  Stack,
  VStack,
} from '@chakra-ui/react'
import { FaRightFromBracket } from 'react-icons/fa6'

import type { DataCombining, Instruction } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import type { CombinerContext } from '@/modules/Mappings/types'
import CombinedEntitySelect from './CombinedEntitySelect'
import { CombinedSchemaLoader } from './CombinedSchemaLoader'
import { DestinationSchemaLoader } from './DestinationSchemaLoader'
import { PrimarySelect } from './PrimarySelect'
import { useTranslation } from 'react-i18next'

export const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()
  const { formData, formContext, uiSchema, registry } = props

  const fieldOptions = getUiOptions(uiSchema)

  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate'>('TitleFieldTemplate', registry, fieldOptions)
  const DescriptionFieldTemplate = getTemplate<'DescriptionFieldTemplate'>(
    'DescriptionFieldTemplate',
    registry,
    fieldOptions
  )

  // TODO[RJSF] Would prefer to reuse the templates; need investigation
  const sourceOptions = getUiOptions(uiSchema?.sources)
  const destOptions = getUiOptions(uiSchema?.destination)
  const primaryOptions = getUiOptions(uiSchema?.sources.primary)
  const destTopicOptions = getUiOptions(uiSchema?.destination.topic)
  const destSchemaOptions = getUiOptions(uiSchema?.destination.schema)

  const sourceError = props.errorSchema?.sources?.__errors
  const primaryError = props.errorSchema?.sources?.primary?.__errors
  const destinationError = props.errorSchema?.destination?.topic?.__errors
  // TODO[RJSF] Create a custom error message, ensuring tag + topic filters >= 1

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw" gap={5}>
          <Box>
            {sourceOptions.title && (
              <TitleFieldTemplate id={'FIXME'} title={sourceOptions.title} schema={props.schema} registry={registry} />
            )}
            {sourceOptions.description && (
              <DescriptionFieldTemplate
                id={'FIXME'}
                description={sourceOptions.description}
                schema={props.schema}
                registry={registry}
              />
            )}
          </Box>
          <Box>
            <FormControl isInvalid={Boolean(sourceError)}>
              <FormLabel>{t('combiner.schema.mappings.sources.combinedData.title')}</FormLabel>
              <CombinedEntitySelect
                tags={formData?.sources?.tags}
                topicFilters={formData?.sources?.topicFilters}
                formContext={formContext}
                onChange={(values) => {
                  if (!formData) return
                  const tag: string[] = []
                  const filter: string[] = []

                  let isPrimary = false
                  values.forEach((entity) => {
                    if (entity.type === DataIdentifierReference.type.TAG) tag.push(entity.value)
                    if (entity.type === DataIdentifierReference.type.TOPIC_FILTER) filter.push(entity.value)
                    if (formData.sources.primary?.type === entity.type && formData.sources.primary?.id === entity.value)
                      isPrimary = true
                  })

                  props.onChange({
                    ...formData,
                    sources: {
                      ...formData.sources,
                      tags: tag,
                      topicFilters: filter,
                      // @ts-ignore TODO[30935] check for type clash on primary
                      primary: isPrimary ? formData.sources.primary : undefined,
                    },
                  })
                }}
              />
              {!sourceError && (
                <FormHelperText>{t('combiner.schema.mappings.sources.combinedData.description')}</FormHelperText>
              )}
              <FormErrorMessage>{sourceError?.join(' ')}</FormErrorMessage>
            </FormControl>
          </Box>
          <VStack overflow={'auto'} alignItems={'flex-start'} justifyContent={'center'} tabIndex={0} gap={5}>
            <FormControl>
              <FormLabel>{t('combiner.schema.mappings.sources.combinedSchema.title')}</FormLabel>
              <CombinedSchemaLoader formData={props.formData} formContext={formContext} />
              {!sourceError && (
                <FormHelperText>{t('combiner.schema.mappings.sources.combinedSchema.description')}</FormHelperText>
              )}
            </FormControl>
          </VStack>
          <Box maxW={'25vw'}>
            <FormControl isInvalid={Boolean(primaryError)}>
              <FormLabel>{primaryOptions.title}</FormLabel>
              <PrimarySelect
                formData={formData}
                onChange={(values) => {
                  if (!props.formData) return

                  props.onChange({
                    ...props.formData,
                    sources: {
                      ...props.formData.sources,
                      // @ts-ignore TODO[30935] check for type clash on primary
                      primary: values
                        ? {
                            id: values.value,
                            type: values.type,
                          }
                        : undefined,
                    },
                  })
                }}
              />
              {!primaryError && <FormHelperText>{primaryOptions.description}</FormHelperText>}
              <FormErrorMessage>{props.errorSchema?.sources?.primary?.__errors}</FormErrorMessage>
            </FormControl>
          </Box>
        </VStack>
        <VStack justifyContent="center">
          <HStack height={38}>
            <Icon as={FaRightFromBracket} />
          </HStack>
        </VStack>
        <VStack flex={1} alignItems="stretch" maxW="50vw">
          <Box>
            {destOptions.title && (
              <TitleFieldTemplate id={'FIXME'} title={destOptions.title} schema={props.schema} registry={registry} />
            )}
            {destOptions.description && (
              <DescriptionFieldTemplate
                id={'FIXME'}
                description={destOptions.description}
                schema={props.schema}
                registry={registry}
              />
            )}
          </Box>
          <Box maxW={'25vw'}>
            <FormControl isInvalid={Boolean(destinationError)}>
              <FormLabel>{destTopicOptions.title}</FormLabel>
              <SelectTopic
                isMulti={false}
                isCreatable={true}
                id={'destination'}
                value={formData?.destination?.topic || null}
                onChange={(topic) => {
                  if (!props.formData) return

                  if (topic && typeof topic === 'string') props.onChange({ ...props.formData, destination: { topic } })
                  else if (!topic) props.onChange({ ...props.formData, destination: { topic: '' } })
                }}
              />
              {!destinationError && <FormHelperText>{destTopicOptions.description}</FormHelperText>}

              <FormErrorMessage>{destinationError}</FormErrorMessage>
            </FormControl>
          </Box>
          <FormControl isInvalid={Boolean(destinationError)}>
            <FormLabel>{destSchemaOptions.title}</FormLabel>
            <DestinationSchemaLoader
              formData={props.formData}
              onChange={(schema) => {
                if (!props.formData) return

                props.onChange({
                  ...props.formData,
                  destination: { topic: props.formData.destination.topic, schema },
                })
              }}
              onChangeInstructions={(v: Instruction[]) => {
                if (!props.formData) return
                if (!v.length) return

                props.onChange({
                  ...props.formData,
                  instructions: v,
                })
              }}
            />
            <FormHelperText>{destSchemaOptions.description}</FormHelperText>
          </FormControl>
        </VStack>
      </Stack>
    </VStack>
  )
}
