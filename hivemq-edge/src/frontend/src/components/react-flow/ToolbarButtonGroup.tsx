import { FC } from 'react'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'

// TODO[NVL] ChakraUI Theme doesn't support ButtonGroup
const ToolbarButtonGroup: FC<ButtonGroupProps> = ({ children, ...rest }) => {
  return (
    <ButtonGroup size="sm" variant="solid" colorScheme="gray" isAttached {...rest}>
      {children}
    </ButtonGroup>
  )
}

export default ToolbarButtonGroup
