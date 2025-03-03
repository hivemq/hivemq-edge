import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { GroupBase, MultiValue, OptionBase } from 'chakra-react-select'
import { Select } from 'chakra-react-select'
import { Box } from '@chakra-ui/react'

import type { DomainTag, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import type { CombinerContext } from '@/modules/Mappings/types'

interface EntityReferenceSelectProps {
  id?: string
  tags?: Array<string>
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

const CombinedEntitySelect: FC<EntityReferenceSelectProps> = ({ id, tags, topicFilters, formContext, onChange }) => {
  const { t } = useTranslation()
  const isLoading = useMemo(() => {
    return formContext?.queries?.some((query) => query.isLoading) || false
  }, [formContext?.queries])

  const allOptions = useMemo(() => {
    if (isLoading) return []

    const combinedOptions =
      formContext?.queries?.reduce<EntityOption[]>((acc, queryResult) => {
        if (!queryResult.data) return acc
        if (!queryResult.data.items.length) return acc
        if ((queryResult.data.items[0] as DomainTag).name) {
          const options = (queryResult.data.items as DomainTag[]).map<EntityOption>((tag, index) => ({
            label: tag.name,
            value: tag.name,
            description: tag.description,
            adapterId: formContext.entities?.[index]?.id,
            type: DataIdentifierReference.type.TAG,
          }))
          acc.push(...options)
        } else if ((queryResult.data.items[0] as TopicFilter).topicFilter) {
          const options = (queryResult.data.items as TopicFilter[]).map<EntityOption>((topicFilter) => ({
            label: topicFilter.topicFilter,
            value: topicFilter.topicFilter,
            description: topicFilter.description,
            type: DataIdentifierReference.type.TAG,
          }))

          acc.push(...options)
        }

        return acc
      }, []) || []

    return combinedOptions.reduce<EntityOption[]>((acc, current) => {
      const isAlreadyIn = acc.find((item) => item.value === current.value && item.type === current.type)
      if (!isAlreadyIn) {
        return acc.concat([current])
      }
      return acc
    }, [])
  }, [formContext?.entities, formContext?.queries, isLoading])

  const values = useMemo(() => {
    const tagValue =
      tags?.map<EntityOption>((value) => ({ value: value, label: value, type: DataIdentifierReference.type.TAG })) || []
    const topicFilter =
      topicFilters?.map<EntityOption>((value) => ({
        value: value,
        label: value,
        type: DataIdentifierReference.type.TOPIC_FILTER,
      })) || []
    return [...tagValue, ...topicFilter]
  }, [tags, topicFilters])

  return (
    <Box maxW={'25vw'}>
      <Select<EntityOption, true, GroupBase<EntityOption>>
        inputId={id}
        id={'combiner-entity-select'}
        instanceId={'entity'}
        options={allOptions}
        isLoading={isLoading}
        isMulti
        value={values}
        aria-label={t('combiner.schema.mappings.sources.description')}
        onChange={(newValue) => {
          if (newValue) onChange(newValue)
        }}
        isClearable
        placeholder={t('combiner.schema.mapping.combinedSelector.placeholder')}
        // noOptionsMessage={() => t('EntityCreatableSelect.options.noOptionsMessage', { context: type })}
        // components={customComponents(isMulti, type)}
        // filterOption={createFilter(filterConfig)}
      />
    </Box>
  )
}

export default CombinedEntitySelect
