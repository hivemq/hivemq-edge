import { ComponentType } from 'react'
import { chakraComponents, GroupBase, OptionProps } from 'chakra-react-select'
import { Icon } from '@chakra-ui/react'
import { SiMqtt } from 'react-icons/si'

import { RangeOption } from '../types.ts'

const Option: ComponentType<OptionProps<RangeOption, false, GroupBase<RangeOption>>> = ({ children, ...props }) => {
  const { value } = props.data

  // Seems to be the best way of detecting the "Create option"
  const isOptionCreate = value === ''

  return (
    <chakraComponents.Option {...props}>
      {!isOptionCreate && <Icon as={SiMqtt} color={props.data.color} mr={2} h={5} w={5} />}
      {children}
    </chakraComponents.Option>
  )
}

export default Option
