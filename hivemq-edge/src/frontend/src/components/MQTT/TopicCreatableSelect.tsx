import { FC } from 'react'
import {
  CreatableSelect,
  createFilter,
  OptionBase,
  SingleValue,
  SelectComponentsConfig,
  GroupBase,
  chakraComponents,
} from 'chakra-react-select'
import { useTranslation } from 'react-i18next'
import TopicIcon from '@/components/Icons/TopicIcon.tsx'

interface TopicOption extends OptionBase {
  label: string
  value: string
  iconColor: string
}

interface TopicSelectProps {
  id: string
  options: string[]
  isLoading: boolean
  value: string
  onChange: (value: string | undefined) => void
}

const filterConfig = {
  trim: false,
}

const customComponents: SelectComponentsConfig<TopicOption, true, GroupBase<TopicOption>> = {
  Option: ({ children, ...props }) => (
    <chakraComponents.Option {...props}>
      {props.data.iconColor && <TopicIcon color={props.data.iconColor} mr={2} h={5} w={5} />}
      {children}
    </chakraComponents.Option>
  ),

  Control: ({ children, ...props }) => (
    <chakraComponents.Control {...props}>
      <TopicIcon mr={0} ml={3} />
      {children}
    </chakraComponents.Control>
  ),
}

const TopicCreatableSelect: FC<TopicSelectProps> = ({ id, options, isLoading, value, onChange }) => {
  const topicOptions = Array.from(new Set([...options]))
    .sort()
    .map<TopicOption>((e) => ({ label: e, value: e, iconColor: 'brand.500' }))
  const { t } = useTranslation('components')

  return (
    <CreatableSelect
      aria-label={t('topicCreate.label') as string}
      placeholder={t('topicCreate.placeholder') as string}
      noOptionsMessage={() => t('topicCreate.options.noOptionsMessage')}
      formatCreateLabel={(e) => t('topicCreate.options.createLabel', { topic: e })}
      isLoading={isLoading}
      id={id}
      isClearable
      isSearchable
      isMulti={false}
      options={topicOptions}
      value={value ? { label: value, value: value, iconColor: 'brand.200' } : undefined}
      onChange={(value) => {
        const newValue = value as SingleValue<TopicOption>
        onChange(newValue?.label)
      }}
      components={customComponents}
      filterOption={createFilter(filterConfig)}
      // @ts-ignore TODO[NVL] Bug with CRS, see https://github.com/csandman/chakra-react-select/issues/273
      selectedOptionStyle="check"
    />
  )
}

export default TopicCreatableSelect
