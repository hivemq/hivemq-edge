import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { GroupBase, MultiValue, OptionBase } from 'chakra-react-select'
import { Select } from 'chakra-react-select'

import { DataCombining } from '@/api/__generated__'
import type { DomainTagList, DomainTag, TopicFilter, TopicFilterList } from '@/api/__generated__'
import type { UseQueryResult } from '@tanstack/react-query'

interface EntityReferenceSelectProps {
  id?: string
  tags?: Array<string>
  topicFilters?: Array<string>
  optionQueries?: UseQueryResult<DomainTagList | TopicFilterList, Error>[]
  onChange: (value: MultiValue<EntityOption>) => void
}

interface EntityOption extends OptionBase {
  label: string
  value: string
  type: string
  description?: string
}

const CombinedEntitySelect: FC<EntityReferenceSelectProps> = ({ tags, topicFilters, optionQueries, onChange }) => {
  const { t } = useTranslation()
  const isLoading = useMemo(() => {
    return optionQueries?.some((query) => query.isLoading) || false
  }, [optionQueries])

  const allOptions = useMemo(() => {
    if (isLoading) return []

    return (
      optionQueries?.reduce<EntityOption[]>((acc, queryResult) => {
        if (!queryResult.data) return acc
        if (!queryResult.data.items.length) return acc
        if ((queryResult.data.items[0] as DomainTag).name) {
          const options = (queryResult.data.items as DomainTag[]).map<EntityOption>((tag) => ({
            label: tag.name,
            value: tag.name,
            description: tag.description,
            type: DataCombining.primaryType.TAG,
          }))
          acc.push(...options)
        } else if ((queryResult.data.items[0] as TopicFilter).topicFilter) {
          const options = (queryResult.data.items as TopicFilter[]).map<EntityOption>((topicFilter) => ({
            label: topicFilter.topicFilter,
            value: topicFilter.topicFilter,
            description: topicFilter.description,
            type: DataCombining.primaryType.TOPIC_FILTER,
          }))

          acc.push(...options)
        }

        return acc
      }, []) || []
    )
  }, [isLoading, optionQueries])

  const values = useMemo(() => {
    const tagValue =
      tags?.map<EntityOption>((value) => ({ value: value, label: value, type: DataCombining.primaryType.TAG })) || []
    const topicFilter =
      topicFilters?.map<EntityOption>((value) => ({
        value: value,
        label: value,
        type: DataCombining.primaryType.TOPIC_FILTER,
      })) || []
    return [...tagValue, ...topicFilter]
  }, [tags, topicFilters])

  return (
    <Select<EntityOption, true, GroupBase<EntityOption>>
      options={allOptions}
      isLoading={isLoading}
      isMulti
      value={values}
      aria-label={t('combiner.schema.mappings.sources.description')}
      onChange={(newValue) => {
        if (newValue) onChange(newValue)
      }}
      // placeholder={t('EntityCreatableSelect.placeholder', { context: type })}
      // noOptionsMessage={() => t('EntityCreatableSelect.options.noOptionsMessage', { context: type })}
      // formatCreateLabel={(entity) => t('EntityCreatableSelect.options.createLabel', { context: type, entity: entity })}
      // id={id}
      // instanceId={type}
      // inputId={`react-select-${type}-input`}
      // isClearable
      // isSearchable
      // selectedOptionStyle="check"
      // components={customComponents(isMulti, type)}
      // filterOption={createFilter(filterConfig)}
    />
  )
}

export default CombinedEntitySelect
