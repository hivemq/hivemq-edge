import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { GroupBase, MultiValue, OptionBase } from 'chakra-react-select'
import { chakraComponents } from 'chakra-react-select'
import { Select } from 'chakra-react-select'
import type { BoxProps } from '@chakra-ui/react'
import { Box } from '@chakra-ui/react'

import type { DomainTag, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { PLCTag, Topic, TopicFilter as TopicFilterComponent } from '@/components/MQTT/EntityTag'
import { SelectEntityType } from '@/components/MQTT/types'
import { formatOwnershipString } from '@/components/MQTT/topic-utils'
import type { CombinerContext } from '@/modules/Mappings/types'
import { CombinerOptionContent } from './CombinerOptionContent'

interface EntityReferenceSelectProps extends Omit<BoxProps, 'onChange'> {
  id?: string
  formContext?: CombinerContext
  onChange: (value: MultiValue<EntityOption>) => void
}

interface EntityOption extends OptionBase {
  label: string
  value: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
  sortKey?: string
}

const CombinedEntitySelect: FC<EntityReferenceSelectProps> = ({ id, formContext, onChange, ...boxProps }) => {
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
            adapterId: entity.id,
            type: DataIdentifierReference.type.TAG,
            sortKey: entity.id ? `${entity.id} :: ${tag.name}` : tag.name,
          }))
          acc.push(...options)
        } else if ((query.data.items[0] as TopicFilter).topicFilter) {
          const options = (query.data.items as TopicFilter[]).map<EntityOption>((topicFilter) => ({
            label: topicFilter.topicFilter,
            value: topicFilter.topicFilter,
            description: topicFilter.description,
            type: DataIdentifierReference.type.TOPIC_FILTER,
            sortKey: topicFilter.topicFilter,
          }))

          acc.push(...options)
        }

        return acc
      }, []) || []

    const deduped = combinedOptions.reduce<EntityOption[]>((acc, current) => {
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

    return deduped.sort((a, b) => {
      const keyA = a.sortKey ?? a.value
      const keyB = b.sortKey ?? b.value
      return keyA.localeCompare(keyB)
    })
  }, [formContext?.entityQueries, isLoading])

  const values = useMemo(() => {
    if (!formContext?.selectedSources) return []

    const tagValue = formContext.selectedSources.tags.map<EntityOption>((ref) => ({
      value: ref.id,
      label: formatOwnershipString(ref),
      type: ref.type,
      adapterId: ref.scope || undefined,
    }))

    const topicFilterValue = formContext.selectedSources.topicFilters.map<EntityOption>((ref) => ({
      value: ref.id,
      label: ref.id,
      type: ref.type,
    }))

    return [...tagValue, ...topicFilterValue]
  }, [formContext?.selectedSources])

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
        filterOption={(option, inputValue) => {
          const lower = inputValue.toLowerCase()
          return (
            option.label.toLowerCase().includes(lower) ||
            (option.data.adapterId?.toLowerCase().includes(lower) ?? false)
          )
        }}
        placeholder={t('combiner.schema.mapping.combinedSelector.placeholder')}
        components={{
          MultiValueContainer: ({ children, ...props }) => (
            <>
              {props.data.type === SelectEntityType.TOPIC && <Topic tagTitle={children} mr={3} />}
              {props.data.type === SelectEntityType.TAG && <PLCTag tagTitle={children} mr={3} />}
              {props.data.type === SelectEntityType.TOPIC_FILTER && <TopicFilterComponent tagTitle={children} mr={3} />}
            </>
          ),
          Option: ({ children: _children, ...props }) => (
            <chakraComponents.Option {...props}>
              <CombinerOptionContent
                label={props.data.label}
                adapterId={props.data.adapterId}
                type={props.data.type}
                description={props.data.description}
              />
            </chakraComponents.Option>
          ),
        }}
      />
    </Box>
  )
}

export default CombinedEntitySelect
