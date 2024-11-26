import { FC, useCallback, useMemo } from 'react'
import {
  chakraComponents,
  CreatableSelect,
  createFilter,
  GroupBase,
  OptionBase,
  Select,
  SelectComponentsConfig,
  OnChangeValue,
  PropsValue,
  MultiValue,
  SingleValue,
} from 'chakra-react-select'
import { HStack, IconProps, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.ts'
import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import { PLCTagIcon, TopicFilterIcon, TopicIcon } from '@/components/Icons/TopicIcon.tsx'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag.tsx'
import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'

export enum SelectEntityType {
  TOPIC = 'TOPIC',
  TOPIC_FILTER = 'TOPIC_FILTER',
  TAG = 'TAG',
}

interface EntityOption extends OptionBase {
  label: string
  value: string
  description?: string
  // __isNew__ is not exposed in the OptionBase
  __isNew__?: boolean
}

const customComponents = (
  isMulti: boolean,
  type: SelectEntityType
): SelectComponentsConfig<EntityOption, boolean, GroupBase<EntityOption>> => ({
  DropdownIndicator: null,
  Option: ({ children, ...props }) => {
    const { __isNew__ } = props.data
    return (
      <chakraComponents.Option {...props}>
        <HStack>
          {type === SelectEntityType.TOPIC && <TopicIcon mr={2} h={5} w={5} data-option-type={type} />}
          {type === SelectEntityType.TAG && <PLCTagIcon mr={2} h={5} w={5} data-option-type={type} />}
          {type === SelectEntityType.TOPIC_FILTER && <TopicFilterIcon mr={2} h={5} w={5} data-option-type={type} />}
          <VStack gap={0} alignItems="flex-start">
            {!__isNew__ && <code>{props.data.label}</code>}
            {__isNew__ && <Text>{props.data.label}</Text>}
            <Text fontSize="sm">{props.data.description}</Text>
          </VStack>
        </HStack>
      </chakraComponents.Option>
    )
  },

  Control: ({ children, ...props }) => (
    <chakraComponents.Control {...props}>
      {!isMulti && type === SelectEntityType.TOPIC && <TopicIcon mr={0} ml={3} data-option-type={type} />}
      {!isMulti && type === SelectEntityType.TAG && <PLCTagIcon mr={0} ml={3} data-option-type={type} />}
      {!isMulti && type === SelectEntityType.TOPIC_FILTER && <TopicFilterIcon mr={0} ml={3} data-option-type={type} />}
      {children}
    </chakraComponents.Control>
  ),

  MultiValueContainer: ({ children, ...props }) => {
    return (
      // TODO[NVL] removing the default style might cause side effects
      <chakraComponents.MultiValueContainer {...props} sx={{}}>
        {type === SelectEntityType.TOPIC && <Topic tagTitle={children} mr={3} />}
        {type === SelectEntityType.TAG && <PLCTag tagTitle={children} mr={3} />}
        {type === SelectEntityType.TOPIC_FILTER && <TopicFilter tagTitle={children} mr={3} />}
      </chakraComponents.MultiValueContainer>
    )
  },
})

interface EntitySelectProps {
  id?: string
  isCreatable?: boolean
  isMulti?: boolean
  type: SelectEntityType
  isLoading: boolean
  options: EntityOption[]
  icon: FC<IconProps>
  onChange: (newValue: OnChangeValue<string, boolean>) => void
  value: PropsValue<string>
}

const EntityCreatableSelect: FC<EntitySelectProps> = ({
  type,
  isCreatable = false,
  isMulti = false,
  options,
  isLoading,
  id,
  onChange,
  value,
}) => {
  const { t } = useTranslation('components')
  const safeValue = useMemo(() => {
    if (value === null) return null
    if (isMulti) return (value as MultiValue<string>).map((value: string) => ({ label: value, value }))
    return { label: value as string, value: value as string }
  }, [isMulti, value])
  const safeOnChange = useCallback(
    (value: MultiValue<EntityOption> | SingleValue<EntityOption>) => {
      if (value === null) {
        onChange(null)
        return
      }
      if (isMulti) {
        onChange((value as MultiValue<EntityOption>).map((option) => option.value))
        return
      }

      onChange((value as EntityOption).value)
    },
    [isMulti, onChange]
  )

  const filterConfig = {
    trim: false,
  }

  const SelectComponent = isCreatable ? CreatableSelect : Select

  return (
    <SelectComponent<EntityOption, boolean, GroupBase<EntityOption>>
      options={options}
      isMulti={isMulti}
      isLoading={isLoading}
      aria-label={t('EntityCreatableSelect.aria-label', { context: type })}
      placeholder={t('EntityCreatableSelect.placeholder', { context: type })}
      noOptionsMessage={() => t('EntityCreatableSelect.options.noOptionsMessage', { context: type })}
      formatCreateLabel={(entity) => t('EntityCreatableSelect.options.createLabel', { context: type, entity: entity })}
      id={id}
      instanceId={type}
      inputId={`react-select-${type}-input`}
      isClearable
      isSearchable
      selectedOptionStyle="check"
      components={customComponents(isMulti, type)}
      filterOption={createFilter(filterConfig)}
      value={safeValue}
      onChange={safeOnChange}
    />
  )
}

export default EntityCreatableSelect

interface CreateSelectableProps {
  id?: string
  isCreatable?: boolean
  isMulti?: boolean
  onChange: (newValue: OnChangeValue<string, boolean>) => void
  value: PropsValue<string>
}

export const SelectTopic: FC<CreateSelectableProps> = (props) => {
  const { data, isLoading } = useGetEdgeTopics({ publishOnly: false })
  const options =
    data.map<EntityOption>((topic) => ({
      label: topic,
      value: topic,
    })) || []

  return (
    <EntityCreatableSelect
      type={SelectEntityType.TOPIC}
      options={options}
      isLoading={isLoading}
      icon={TopicIcon}
      {...props}
    />
  )
}

export const SelectTopicFilter: FC<CreateSelectableProps> = (props) => {
  const { data, isLoading } = useListTopicFilters()
  const options =
    data?.items?.map<EntityOption>((filter) => ({
      label: filter.topicFilter,
      value: filter.topicFilter,
      description: filter.description,
    })) || []

  return (
    <EntityCreatableSelect
      type={SelectEntityType.TOPIC_FILTER}
      options={options}
      isLoading={isLoading}
      icon={PLCTagIcon}
      {...props}
    />
  )
}

interface TagSelectProps extends CreateSelectableProps {
  adapterId: string
}

export const SelectTag: FC<TagSelectProps> = ({ adapterId, ...rest }) => {
  const { data, isLoading } = useGetDomainTags(adapterId)
  const options = data?.items?.map<EntityOption>((tag) => ({ label: tag.tagName, value: tag.tagName })) || []

  return (
    <EntityCreatableSelect
      type={SelectEntityType.TAG}
      options={options}
      isLoading={isLoading}
      icon={PLCTagIcon}
      {...rest}
    />
  )
}
