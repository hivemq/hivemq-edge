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
import { SiMqtt } from 'react-icons/si'
import { Icon } from '@chakra-ui/react'

interface TopicOption extends OptionBase {
  label: string
  value: string
  Icon: FC
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
  // ValueContainer: ({ children, ...p }) => (
  //   <chakraComponents.ValueContainer {...p} className={'ZZZZZZZZ'}>
  //     {children}
  //   </chakraComponents.ValueContainer>
  // ),
  // Input: (props) => <chakraComponents.Input {...props} className={'XXXXXX'} />,
  Option: ({ children, ...props }) => (
    <chakraComponents.Option {...props}>
      {props.data.Icon && (
        <Icon as={props.data.Icon} color={props.data.iconColor} aria-label={'XXXXX'} mr={2} h={5} w={5} />
      )}
      {children}
    </chakraComponents.Option>
  ),
  // SingleValue: (props) => {
  //   return (
  //     // <HStack>
  //     //   <TagLeftIcon as={props.data.Icon} color={props.data.iconColor} />
  //     <chakraComponents.SingleValue {...props} className={'YYYYY'}>
  //       {props.children}
  //     </chakraComponents.SingleValue>
  //     // {/*</HStack>*/}
  //   )
  // },
  // MultiValueContainer: ({ children, ...props }) => (
  //   <chakraComponents.MultiValueContainer {...props}>
  //     <TagLeftIcon as={props.data.Icon} color={props.data.iconColor} />
  //     {children}
  //   </chakraComponents.MultiValueContainer>
  // ),
}

const TopicCreatableSelect: FC<TopicSelectProps> = ({ id, options, isLoading, value, onChange }) => {
  const topicOptions = Array.from(new Set([...options]))
    .sort()
    .map<TopicOption>((e) => ({ label: e, value: e, Icon: SiMqtt, iconColor: 'brand.500' }))

  return (
    <CreatableSelect
      aria-label={'Add a topic'}
      placeholder={'Type or select ...'}
      noOptionsMessage={() => 'No topic loaded'}
      formatCreateLabel={(e) => `Add the topic ... ${e}`}
      isLoading={isLoading}
      id={id}
      isClearable
      isSearchable
      isMulti={false}
      options={topicOptions}
      value={value ? { label: value, value: value, Icon: SiMqtt, iconColor: 'brand.200' } : undefined}
      onChange={(value) => {
        const newValue = value as SingleValue<TopicOption>
        onChange(newValue?.label)
      }}
      components={customComponents}
      filterOption={createFilter(filterConfig)}
    />
  )
}

export default TopicCreatableSelect
