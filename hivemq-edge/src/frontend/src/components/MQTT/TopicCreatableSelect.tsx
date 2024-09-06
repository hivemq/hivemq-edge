import { RefAttributes } from 'react'
import {
  CreatableSelect,
  createFilter,
  OptionBase,
  SingleValue,
  SelectComponentsConfig,
  GroupBase,
  CreatableProps,
  SelectInstance,
  chakraComponents,
  Select,
} from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import { TopicIcon, PLCTagIcon } from '@/components/Icons/TopicIcon.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'

interface TopicOption extends OptionBase {
  label: string
  value: string
  iconColor: string
}

const customComponents = (
  isMulti: boolean,
  isTag?: boolean
): SelectComponentsConfig<TopicOption, never, GroupBase<TopicOption>> => ({
  DropdownIndicator: null,
  Option: ({ children, ...props }) => (
    <chakraComponents.Option {...props}>
      {!isTag && <TopicIcon mr={2} h={5} w={5} />}
      {isTag && <PLCTagIcon mr={2} h={5} w={5} />}
      {children}
    </chakraComponents.Option>
  ),

  Control: ({ children, ...props }) => (
    <chakraComponents.Control {...props}>
      {!isMulti && !isTag && <TopicIcon mr={0} ml={3} />}
      {!isMulti && isTag && <PLCTagIcon mr={0} ml={3} />}
      {children}
    </chakraComponents.Control>
  ),

  MultiValueContainer: ({ children, ...props }) => {
    // TODO[NVL] removing the default style might cause side effects
    return (
      <chakraComponents.MultiValueContainer {...props} sx={{}}>
        <Topic tagTitle={children} mr={3} />
      </chakraComponents.MultiValueContainer>
    )
  },
})

// Recreating the type for the CreatableSelect component
type TopicCreatableSelect<IsMulti extends boolean> = CreatableProps<TopicOption, IsMulti, GroupBase<TopicOption>> &
  RefAttributes<SelectInstance<TopicOption, IsMulti, GroupBase<TopicOption>>>

interface TopicCreatableSelectProps<IsMulti extends boolean>
  extends Partial<Omit<TopicCreatableSelect<IsMulti>, 'options'>> {
  id: string
  options: string[]
  isCreatable?: boolean
  isTag?: boolean
}

const AbstractTopicCreatableSelect = <T extends boolean>({
  id,
  options,
  isLoading,
  isMulti,
  isTag = false,
  isCreatable = true,
  ...rest
}: TopicCreatableSelectProps<T>) => {
  const topicOptions = Array.from(new Set([...options]))
    .sort()
    .map<TopicOption>((e) => ({ label: e, value: e, iconColor: 'brand.500' }))
  const { t } = useTranslation('components')

  const filterConfig = {
    trim: false,
  }

  const SelectComponent = isCreatable ? CreatableSelect : Select

  return (
    <SelectComponent<TopicOption, T, GroupBase<TopicOption>>
      aria-label={t('topicCreate.label')}
      placeholder={t('topicCreate.placeholder')}
      noOptionsMessage={() => t('topicCreate.options.noOptionsMessage')}
      formatCreateLabel={(topic) => t('topicCreate.options.createLabel', { topic: topic })}
      isLoading={isLoading}
      id={id}
      isClearable
      isSearchable
      isMulti={isMulti}
      options={topicOptions}
      // @ts-ignore Need to check the type
      components={isMulti === undefined ? undefined : customComponents(isMulti, isTag)}
      filterOption={createFilter(filterConfig)}
      selectedOptionStyle="check"
      {...rest}
    />
  )
}

interface SingleTopicCreatableSelectProps extends Omit<TopicCreatableSelectProps<false>, 'value' | 'onChange'> {
  value: string
  onChange: (value: string | undefined) => void
}

export const SingleTopicCreatableSelect = ({ value, onChange, isTag, ...props }: SingleTopicCreatableSelectProps) => (
  <AbstractTopicCreatableSelect<false>
    {...props}
    isMulti={false}
    isTag={isTag}
    value={value ? { label: value, value: value, iconColor: 'brand.200' } : undefined}
    onChange={(value) => {
      const newValue = value as SingleValue<TopicOption>
      onChange(newValue?.label)
    }}
  />
)

interface MultiTopicsCreatableSelectProps
  extends Omit<TopicCreatableSelectProps<true>, 'value' | 'onChange' | 'options'> {
  value: string[]
  onChange: (value: string[] | undefined) => void
  isCreatable?: boolean
}

export const MultiTopicsCreatableSelect = ({ value, onChange, isTag, ...props }: MultiTopicsCreatableSelectProps) => {
  const { data, isSuccess } = useGetEdgeTopics({ publishOnly: false })

  return (
    <AbstractTopicCreatableSelect<true>
      {...props}
      options={data}
      isLoading={!isSuccess}
      isMulti={true}
      isTag={isTag}
      value={value?.map<TopicOption>((e) => ({ label: e, value: e, iconColor: 'brand.200' }))}
      onChange={(m) => onChange(m.map<string>((e) => e.value))}
    />
  )
}
