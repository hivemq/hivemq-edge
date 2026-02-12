import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { GroupBase, MultiValue, OptionBase } from 'chakra-react-select'
import { chakraComponents } from 'chakra-react-select'
import { Select } from 'chakra-react-select'
import type { BoxProps } from '@chakra-ui/react'
import { Box, HStack, Text, VStack } from '@chakra-ui/react'

import type { DomainTag, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { PLCTag, Topic, TopicFilter as TopicFilterComponent } from '@/components/MQTT/EntityTag'
import { SelectEntityType } from '@/components/MQTT/types'
import type { CombinerContext } from '@/modules/Mappings/types'

interface EntityReferenceSelectProps extends Omit<BoxProps, 'onChange'> {
  id?: string
  /**
   * @deprecated Use formContext.selectedSources instead. This prop is kept for backward compatibility during migration.
   */
  tags?: Array<string>
  /**
   * @deprecated Use formContext.selectedSources instead. This prop is kept for backward compatibility during migration.
   */
  topicFilters?: Array<string>
  formContext?: CombinerContext
  onChange: (value: MultiValue<EntityOption>) => void
}

interface EntityOption extends OptionBase {
  label: string
  value: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
}

const CombinedEntitySelect: FC<EntityReferenceSelectProps> = ({
  id,
  tags,
  topicFilters,
  formContext,
  onChange,
  ...boxProps
}) => {
  const { t } = useTranslation()
  const isLoading = useMemo(() => {
    return formContext?.entityQueries?.some((eq) => eq?.query?.isLoading) || false
  }, [formContext?.entityQueries])

  const allOptions = useMemo(() => {
    if (isLoading) return []

    const combinedOptions =
      formContext?.entityQueries?.reduce<EntityOption[]>((acc, entityQuery) => {
        // Safety check: handle undefined entityQuery or query
        if (!entityQuery || !entityQuery.query) return acc

        const { entity, query } = entityQuery

        if (!query.data) return acc
        if (!query.data.items.length) return acc

        if ((query.data.items[0] as DomainTag).name) {
          const options = (query.data.items as DomainTag[]).map<EntityOption>((tag) => ({
            label: tag.name,
            value: tag.name,
            description: tag.description,
            adapterId: entity.id, // ✅ Direct access to entity, no index needed
            type: DataIdentifierReference.type.TAG,
          }))
          acc.push(...options)
        } else if ((query.data.items[0] as TopicFilter).topicFilter) {
          const options = (query.data.items as TopicFilter[]).map<EntityOption>((topicFilter) => ({
            label: topicFilter.topicFilter,
            value: topicFilter.topicFilter,
            description: topicFilter.description,
            type: DataIdentifierReference.type.TOPIC_FILTER,
          }))

          acc.push(...options)
        }

        return acc
      }, []) || []

    return combinedOptions.reduce<EntityOption[]>((acc, current) => {
      // Check for duplicates by value, type, AND adapterId (scope)
      // This allows tags with same name from different adapters
      const isAlreadyIn = acc.find(
        (item) => item.value === current.value && item.type === current.type && item.adapterId === current.adapterId
      )
      if (!isAlreadyIn) {
        return acc.concat([current])
      }
      return acc
    }, [])
  }, [formContext?.entityQueries, isLoading])

  const values = useMemo(() => {
    // Prefer selectedSources from context (Option B implementation)
    if (formContext?.selectedSources) {
      const tagValue = formContext.selectedSources.tags.map<EntityOption>((ref) => ({
        value: ref.id,
        label: ref.id,
        type: ref.type,
        adapterId: ref.scope || undefined, // Include scope for display
      }))

      const topicFilterValue = formContext.selectedSources.topicFilters.map<EntityOption>((ref) => ({
        value: ref.id,
        label: ref.id,
        type: ref.type,
      }))

      return [...tagValue, ...topicFilterValue]
    }

    // Backward compatibility: fall back to deprecated props during migration
    const tagValue =
      tags?.map<EntityOption>((value) => ({ value: value, label: value, type: DataIdentifierReference.type.TAG })) || []
    const topicFilter =
      topicFilters?.map<EntityOption>((value) => ({
        value: value,
        label: value,
        type: DataIdentifierReference.type.TOPIC_FILTER,
      })) || []
    return [...tagValue, ...topicFilter]
  }, [formContext?.selectedSources, tags, topicFilters])

  return (
    <Box {...boxProps}>
      <Select<EntityOption, true, GroupBase<EntityOption>>
        inputId={id}
        id="combiner-entity-select"
        instanceId="entity"
        options={allOptions}
        isLoading={isLoading}
        isMulti
        value={values}
        aria-label={t('Combiner.mappings.items.sources.description', { ns: 'schemas' })}
        // Make options unique by combining value + adapterId + type
        // This allows tags with the same name from different adapters to coexist
        getOptionValue={(option) => `${option.value}@${option.adapterId || 'null'}@${option.type}`}
        onChange={(newValue) => {
          if (newValue) onChange(newValue)
        }}
        isClearable
        placeholder={t('combiner.schema.mapping.combinedSelector.placeholder')}
        components={{
          MultiValueContainer: ({ children, ...props }) => (
            <>
              {props.data.type === SelectEntityType.TOPIC && <Topic tagTitle={children} mr={3} />}
              {props.data.type === SelectEntityType.TAG && <PLCTag tagTitle={children} mr={3} />}
              {props.data.type === SelectEntityType.TOPIC_FILTER && <TopicFilterComponent tagTitle={children} mr={3} />}
            </>
          ),
          Option: ({ children, ...props }) => {
            return (
              <chakraComponents.Option {...props}>
                <VStack gap={0} alignItems="stretch" w="100%">
                  <HStack>
                    <Box flex={1}>
                      <Text flex={1}>{props.data.label}</Text>
                    </Box>
                    <Box>
                      <Text fontSize="sm" fontWeight="bold">
                        {t('combiner.schema.mapping.combinedSelector.type', { context: props.data.type })}
                      </Text>
                    </Box>
                  </HStack>
                  <Text fontSize="sm" noOfLines={3} ml={4} lineHeight="normal" textAlign="justify">
                    {props.data.description}
                  </Text>
                </VStack>
              </chakraComponents.Option>
            )
          },
        }}
      />
    </Box>
  )
}

export default CombinedEntitySelect
