import { FC } from 'react'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'

// TODO[NVL] ChakraUI Theme doesn't support ButtonGroup
const WorkspaceButtonGroup: FC<ButtonGroupProps> = ({ children, ...rest }) => {
  return (
    <ButtonGroup
      size="sm"
      variant="solid"
      colorScheme="blue"
      orientation="vertical"
      isAttached
      sx={{
        boxShadow: '0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19)',
        backgroundColor: 'rgba(0, 0, 0, 0.19)',
      }}
      {...rest}
    >
      {children}
    </ButtonGroup>
  )
}

export default WorkspaceButtonGroup
