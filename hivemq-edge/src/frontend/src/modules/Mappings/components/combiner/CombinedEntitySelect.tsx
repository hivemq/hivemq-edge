import type { FC } from 'react'
import { useMemo } from 'react'
import type { GroupBase, OptionBase } from 'chakra-react-select'
import { Select } from 'chakra-react-select'

import type { DomainTag, TopicFilter, TopicFilterList } from '@/api/__generated__'
import type { DomainTagList } from '@/api/__generated__'
import type { UseQueryResult } from '@tanstack/react-query'

interface EntityReferenceSelectProps {
  id?: string
  tags?: Array<string>
  topicFilters?: Array<string>
  options?: UseQueryResult<DomainTagList | TopicFilterList, Error>[]
}

interface EntityOption extends OptionBase {
  label: string
  value: string
  type: string
  description?: string
}

const CombinedEntitySelect: FC<EntityReferenceSelectProps> = ({ tags, topicFilters, options }) => {
  const isLoading = useMemo(() => {
    return options?.some((e) => e.isLoading) || false
  }, [options])

  const allOptions = useMemo(() => {
    if (isLoading) return []

    return (
      options?.reduce<EntityOption[]>((acc, v) => {
        if (!v.data) return acc
        if (!v.data.items.length) return acc
        if ((v.data.items[0] as DomainTag).name) {
          const ggf = (v.data.items as DomainTag[]).map<EntityOption>((e) => ({
            label: e.name,
            value: e.name,
            description: e.description,
            type: 'TAG',
          }))

          acc.push(...ggf)
        } else if ((v.data.items[0] as TopicFilter).topicFilter) {
          const ggf = (v.data.items as TopicFilter[]).map<EntityOption>((e) => ({
            label: e.topicFilter,
            value: e.topicFilter,
            description: e.description,
            type: 'TOPIC_FILTER',
          }))

          acc.push(...ggf)
        }

        return acc
      }, []) || []
    )
  }, [isLoading, options])

  const values = useMemo(() => {
    const tagValue = tags?.map<EntityOption>((e) => ({ value: e, label: e, type: 'TAG' })) || []
    const topicFilter = topicFilters?.map<EntityOption>((e) => ({ value: e, label: e, type: 'TOPIC_FILTER' })) || []
    return [...tagValue, ...topicFilter]
  }, [tags, topicFilters])

  return (
    <Select<EntityOption, boolean, GroupBase<EntityOption>>
      options={allOptions}
      isMulti
      isLoading={isLoading}
      value={values}
    />
  )
}

export default CombinedEntitySelect
