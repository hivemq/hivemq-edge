import type { FC } from 'react'
import type { ButtonGroupProps } from '@chakra-ui/react'
import { ButtonGroup } from '@chakra-ui/react'

// TODO[NVL] ChakraUI Theme doesn't support ButtonGroup
const ToolbarButtonGroup: FC<ButtonGroupProps> = ({ children, ...rest }) => {
  return (
    <ButtonGroup size="sm" variant="solid" colorScheme="gray" {...rest}>
      {children}
    </ButtonGroup>
  )
}

export default ToolbarButtonGroup
