import { ComponentType } from 'react'
import { chakraComponents, GroupBase, OptionProps } from 'chakra-react-select'
import { Badge, Box, Button, HStack, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { RangeOption } from '../types.ts'
import { badgeFrom } from '../utils/range-option.utils.ts'

const Option: ComponentType<OptionProps<RangeOption, false, GroupBase<RangeOption>>> = ({ children, ...props }) => {
  const { t } = useTranslation('components')
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

  const badge = badgeFrom(props.data, t)

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
