import type { FC } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getTemplate, getUiOptions, type FieldProps, type RJSFSchema } from '@rjsf/utils'
import {
  ButtonGroup,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Grid,
  GridItem,
  VStack,
} from '@chakra-ui/react'

import type { DataCombining, Instruction } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import type { CombinerContext, SelectedSources } from '@/modules/Mappings/types'
import { reconstructSelectedSources } from '@/modules/Mappings/utils/combining.utils'
import CombinedEntitySelect from './CombinedEntitySelect'
import { CombinedSchemaLoader } from './CombinedSchemaLoader'
import { AutoMapping } from './components/AutoMapping'
import { ClearMappings } from './components/ClearMappings'
import { DestinationSchemaLoader } from './DestinationSchemaLoader'
import { PrimarySelect } from './PrimarySelect'

export const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()
  const { formData, formContext, uiSchema, registry } = props

  // Per-mapping selectedSources state (not shared across mappings)
  // This prevents showing tags from mapping1 when editing mapping2
  const [selectedSources, setSelectedSources] = useState<SelectedSources | undefined>(undefined)

  // Detect if queries are currently loaded (at least one query has data)
  const queriesAreLoaded = useMemo(() => {
    if (!formContext?.entityQueries?.length) return false
    return formContext.entityQueries.some((eq) => eq.query.data !== undefined)
  }, [formContext?.entityQueries])

  // Initialize selectedSources for this specific mapping
  useEffect(() => {
    if (!formData) {
      setSelectedSources({ tags: [], topicFilters: [] })
      return
    }

    // Reconstruct from this mapping's instructions
    // Runs when:
    // 1. Mapping ID changes (editing different mapping)
    // 2. Queries become available (fixes race condition where Strategy 3 context lookup
    //    fails because queries haven't loaded yet on initial render)
    setSelectedSources(() => reconstructSelectedSources(formData, formContext))

    // Only re-run when:
    // - formData.id changes (different mapping) - prevents cascade during edits
    // - queriesAreLoaded changes (false→true transition) - fixes race condition
    // Does NOT re-run on every query update (dataUpdatedAt) to avoid cascade
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formData?.id, queriesAreLoaded])

  // Create local context for this specific mapping
  const localContext = useMemo((): CombinerContext => {
    return {
      ...formContext,
      selectedSources,
      onSelectedSourcesChange: setSelectedSources,
    }
  }, [formContext, selectedSources])

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
  const primaryOptions = getUiOptions(uiSchema?.sources?.primary)
  const destTopicOptions = getUiOptions(uiSchema?.destination?.topic)
  const destSchemaOptions = getUiOptions(uiSchema?.destination?.schema)

  const sourceErrors = props.errorSchema?.sources?.__errors
  const primaryErrors = props.errorSchema?.sources?.primary?.__errors
  const destinationErrors = props.errorSchema?.destination?.topic?.__errors
  const destinationSchemaErrors = props.errorSchema?.destination?.schema?.__errors

  const hasError = (field?: string[]) => {
    return Boolean(field?.length)
  }

  const isDestinationAsset = formData?.destination.assetId

  return (
    <Grid templateColumns="1fr repeat(2, 1px) 1fr" rowGap={4} columnGap={6}>
      <GridItem colSpan={2} data-testid="combining-editor-source-header">
        {sourceOptions.title && (
          <TitleFieldTemplate
            id="root_sources__title"
            title={sourceOptions.title}
            schema={props.schema}
            registry={registry}
          />
        )}
        {sourceOptions.description && (
          <DescriptionFieldTemplate
            id="root_sources__description"
            description={sourceOptions.description}
            schema={props.schema}
            registry={registry}
          />
        )}
      </GridItem>
      <GridItem colSpan={2} data-testid="combining-editor-destination-header">
        {destOptions.title && (
          <TitleFieldTemplate
            id="root_destination__title"
            title={destOptions.title}
            schema={props.schema}
            registry={registry}
          />
        )}
        {destOptions.description && (
          <DescriptionFieldTemplate
            id="root_destination__description"
            description={destOptions.description}
            schema={props.schema}
            registry={registry}
          />
        )}
      </GridItem>
      <GridItem colSpan={2} data-testid="combining-editor-sources-attributes">
        <FormControl isInvalid={hasError(sourceErrors)}>
          <FormLabel>{t('combiner.schema.mappings.sources.combinedData.title')}</FormLabel>
          <CombinedEntitySelect
            // TODO[DEPRECATED]: Remove these props when API fields are removed
            // Currently needed for backward compatibility during transition
            tags={formData?.sources?.tags}
            topicFilters={formData?.sources?.topicFilters}
            formContext={localContext}
            onChange={(values) => {
              if (!formData) return

              // Build DataIdentifierReference arrays with full ownership info
              const tagsWithScope = values
                .filter((entity) => entity.type === DataIdentifierReference.type.TAG)
                .map((entity) => ({
                  id: entity.value,
                  type: DataIdentifierReference.type.TAG,
                  scope: entity.adapterId || null, // ✅ Preserve scope from selection
                }))

              const topicFiltersWithScope = values
                .filter((entity) => entity.type === DataIdentifierReference.type.TOPIC_FILTER)
                .map((entity) => ({
                  id: entity.value,
                  type: DataIdentifierReference.type.TOPIC_FILTER,
                  scope: null, // ✅ Topic filters always null
                }))

              // Update selectedSources state (Option B strategy)
              if (localContext?.onSelectedSourcesChange) {
                localContext.onSelectedSourcesChange({
                  tags: tagsWithScope,
                  topicFilters: topicFiltersWithScope,
                })
              }

              // TEMPORARY: Also update deprecated API fields during transition
              const tags = tagsWithScope.map((t) => t.id)
              const filters = topicFiltersWithScope.map((tf) => tf.id)

              const isPrimary = values.some(
                (entity) =>
                  formData.sources.primary?.type === entity.type && formData.sources.primary?.id === entity.value
              )

              props.onChange({
                ...formData,
                sources: {
                  ...formData.sources,
                  tags: tags,
                  topicFilters: filters,
                  // @ts-ignore TODO[30935] check for type clash on primary
                  primary: isPrimary ? formData.sources.primary : undefined,
                },
              })
            }}
          />
          {!hasError(sourceErrors) && (
            <FormHelperText>{t('combiner.schema.mappings.sources.combinedData.description')}</FormHelperText>
          )}
          <FormErrorMessage>{sourceErrors?.join(' ')}</FormErrorMessage>
        </FormControl>
      </GridItem>
      <GridItem colSpan={2} data-testid="combining-editor-destination-topic">
        <FormControl isInvalid={hasError(destinationErrors)}>
          <FormLabel>{destTopicOptions.title}</FormLabel>
          {!isDestinationAsset && (
            <SelectTopic
              isMulti={false}
              isCreatable={true}
              id="destination"
              value={formData?.destination?.topic || null}
              onChange={(topic) => {
                if (!props.formData) return

                if (topic && typeof topic === 'string') props.onChange({ ...props.formData, destination: { topic } })
                else if (!topic) props.onChange({ ...props.formData, destination: { topic: '' } })
              }}
            />
          )}
          {isDestinationAsset && <Topic tagTitle={formData?.destination?.topic} />}
          {!hasError(destinationErrors) && <FormHelperText>{destTopicOptions.description}</FormHelperText>}
          <FormErrorMessage>{destinationErrors}</FormErrorMessage>
        </FormControl>
      </GridItem>
      <GridItem data-testid="combining-editor-sources-schemas">
        <FormControl>
          <FormLabel mt={1}>{t('combiner.schema.mappings.sources.combinedSchema.title')}</FormLabel>
          <CombinedSchemaLoader formData={props.formData} formContext={formContext} />
          {!hasError(sourceErrors) && (
            <FormHelperText>{t('combiner.schema.mappings.sources.combinedSchema.description')}</FormHelperText>
          )}
        </FormControl>
      </GridItem>
      <GridItem>
        <VStack height="100%" justifyContent="center">
          <ButtonGroup size="sm" flexDirection="column" alignItems="flex-end" gap={2}>
            <AutoMapping
              formData={props.formData}
              formContext={localContext}
              onChange={(instructions: Instruction[]) => {
                if (!props.formData) return

                props.onChange({
                  ...props.formData,
                  instructions: instructions,
                })
              }}
            />
            <ClearMappings
              formData={props.formData}
              onChange={(instructions: Instruction[]) => {
                if (!props.formData) return

                props.onChange({
                  ...props.formData,
                  instructions: instructions,
                })
              }}
            />
          </ButtonGroup>
        </VStack>
      </GridItem>
      <GridItem colSpan={2} data-testid="combining-editor-destination-schema">
        <DestinationSchemaLoader
          isInvalid={hasError(destinationSchemaErrors)}
          isEditable={!isDestinationAsset}
          title={destSchemaOptions.title}
          description={destSchemaOptions.description}
          formData={props.formData}
          formContext={formContext}
          onChange={(schema, instructions) => {
            if (!props.formData) return

            props.onChange({
              ...props.formData,
              destination: { topic: props.formData.destination.topic, schema },
              instructions: instructions || [],
            })
          }}
          onChangeInstructions={(v: Instruction[]) => {
            if (!props.formData) return

            props.onChange({
              ...props.formData,
              instructions: v,
            })
          }}
        />
      </GridItem>
      <GridItem colSpan={2} data-testid="combining-editor-sources-primary">
        <FormControl isInvalid={hasError(primaryErrors)}>
          <FormLabel>{primaryOptions.title}</FormLabel>
          <PrimarySelect
            formData={formData}
            formContext={localContext}
            id="mappings-primary"
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
                        scope: values.adapterId ?? null,
                      }
                    : undefined,
                },
              })
            }}
          />
          {!hasError(primaryErrors) && <FormHelperText>{primaryOptions.description}</FormHelperText>}
          <FormErrorMessage>{props.errorSchema?.sources?.primary?.__errors}</FormErrorMessage>
        </FormControl>
      </GridItem>
    </Grid>
  )
}
