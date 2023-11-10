import { ComponentType } from 'react'
import { chakraComponents, GroupBase, OptionProps } from 'chakra-react-select'
import { Badge, Box, Button, HStack, Text } from '@chakra-ui/react'

import { RangeOption } from '../types.ts'

const Option: ComponentType<OptionProps<RangeOption, false, GroupBase<RangeOption>>> = ({ children, ...props }) => {
  const { value, label, isCommand } = props.data

  // Seems to be the best way of detecting the "Create option"
  const isOptionCreate = value === ''

  if (isCommand) {
    return (
      <Box w={'100%'}>
        <Button variant={'ghost'} size={'sm'} isDisabled>
          {label}
        </Button>
      </Box>
    )
  }

  // TODO[NVL] This is not i18n-friendly
  const duration = props.data.duration?.toObject()
  let badge = ''
  if (duration) {
    const [a, v] = Object.entries({ ...duration })[0]
    badge = `${v}${a[0]}`
  }

  return (
    <chakraComponents.Option {...props}>
      <HStack flexWrap={'nowrap'}>
        {!isOptionCreate && badge && (
          <Badge
            as={'p'}
            textAlign={'center'}
            textTransform={'lowercase'}
            size={'sm'}
            data-testid={`dateRange-option-badge-${value}`}
            variant="solid"
            colorScheme={props.data.colorScheme}
            mr={2}
          >
            {badge}
          </Badge>
        )}
        <Text>{children}</Text>
      </HStack>
    </chakraComponents.Option>
  )
}

export default Option
