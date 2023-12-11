import { FC } from 'react'
import { type BoxProps, useTheme, VStack } from '@chakra-ui/react'

interface NodeWrapperProps extends BoxProps {
  isSelected?: boolean
}

const NodeWrapper: FC<NodeWrapperProps> = ({ children, isSelected = false, ...rest }) => {
  const { colors } = useTheme()

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'dark-lg',
    bg: '#dddfe2',
  }

  return (
    <VStack
      p={6}
      rounded={'md'}
      borderColor={'#bec3c9'}
      backgroundColor={colors.white}
      border={`1px`}
      _hover={{ bg: '#ebedf0' }}
      {...(isSelected ? { ...selectedStyle } : {})}
      {...rest}
    >
      {children}
    </VStack>
  )
}

export default NodeWrapper
