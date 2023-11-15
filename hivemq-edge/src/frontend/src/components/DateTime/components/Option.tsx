import { ComponentType } from 'react'
import { chakraComponents, GroupBase, OptionProps } from 'chakra-react-select'
import { HStack, Text } from '@chakra-ui/react'

import { RangeOption } from '../types.ts'
import OptionBadge from './OptionBadge.tsx'
import OptionCommand from './OptionCommand.tsx'

const Option: ComponentType<OptionProps<RangeOption, false, GroupBase<RangeOption>>> = ({ children, ...props }) => {
  const { value, isCommand } = props.data

  // Seems to be the best way of detecting the "Create option"
  const isOptionCreate = value === ''

  if (isCommand) {
    return <OptionCommand data={props.data} />
  }

  return (
    <chakraComponents.Option {...props}>
      <HStack flexWrap={'nowrap'}>
        {!isOptionCreate && <OptionBadge data={props.data} />}
        <Text>{children}</Text>
      </HStack>
    </chakraComponents.Option>
  )
}

export default Option
