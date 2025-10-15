import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormLabel, Text } from '@chakra-ui/react'
import { chakraComponents, type MultiValue, type MultiValueProps, type OptionProps, Select } from 'chakra-react-select'

import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag.tsx'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'
import type { FilterTopicsOption } from '@/modules/Workspace/components/filters/types.ts'

interface FilterTopicsProps {
  onChange?: (values: MultiValue<FilterTopicsOption>) => void
  value?: MultiValue<FilterTopicsOption>
}

const Option = (props: OptionProps<FilterTopicsOption>) => {
  const { type, value } = props.data
  return (
    <chakraComponents.Option {...props}>
      {type === SelectEntityType.TOPIC && <Topic tagTitle={value} mr={3} />}
      {type === SelectEntityType.TAG && <PLCTag tagTitle={value} mr={3} />}
      {type === SelectEntityType.TOPIC_FILTER && <TopicFilter tagTitle={value} mr={3} />}{' '}
    </chakraComponents.Option>
  )
}

const FilterTopics: FC<FilterTopicsProps> = ({ onChange, value }) => {
  const { t } = useTranslation()

  const { tags, northMappings, isLoading, isError } = useGetDomainOntology()

  const options = useMemo<FilterTopicsOption[]>(() => {
    const allTags =
      tags.data?.items?.map<FilterTopicsOption>((filter) => ({
        label: filter.name,
        value: filter.name,
        type: SelectEntityType.TAG,
      })) || []
    const allTopics =
      northMappings.data?.items?.map<FilterTopicsOption>((filter) => ({
        label: filter.topic,
        value: filter.topic,
        type: SelectEntityType.TOPIC,
      })) || []

    return [...allTopics, ...allTags].sort((a, b) => a.value.localeCompare(b.value))
  }, [northMappings.data?.items, tags.data?.items])

  const handleChange = (values: MultiValue<FilterTopicsOption>) => {
    onChange?.(values)
  }

  return (
    <FormControl variant="horizontal" id="workspace-filter-topics">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-topics-input">
        {t('workspace.searchToolbox.byTopics.label')}
      </FormLabel>
      <Select<FilterTopicsOption, true>
        isClearable
        isMulti
        id="workspace-filter-topics-trigger"
        inputId="workspace-filter-topics-input"
        instanceId="topics"
        isLoading={isLoading}
        isInvalid={isError}
        options={options}
        value={value}
        getOptionValue={(option) => option.value}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byTopics.placeholder')}
        size="sm"
        noOptionsMessage={() => t('workspace.searchToolbox.byTopics.noOptions')}
        chakraStyles={{
          container: filterContainerStyle,
        }}
        components={{
          Option,
          MultiValue: (props: MultiValueProps<FilterTopicsOption, true>) => (
            <chakraComponents.MultiValue {...props}>
              <Text data-testid="workspace-filter-topics-values">{props.data.label}</Text>
            </chakraComponents.MultiValue>
          ),
        }}
      />
    </FormControl>
  )
}

export default FilterTopics
