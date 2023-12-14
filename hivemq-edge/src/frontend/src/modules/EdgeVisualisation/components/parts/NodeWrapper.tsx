import { type BoxProps, VStack, useTheme } from '@chakra-ui/react'
import { FC } from 'react'

interface NodeWrapperProps extends BoxProps {
  isSelected?: boolean
}

const NodeWrapper: FC<NodeWrapperProps> = ({ children, isSelected = false, ...rest }) => {
  const { colors } = useTheme()

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'dark-lg',
    rounded: 'md',
    bg: '#dddfe2',
  }

  return (
    <VStack
      p={'24px'}
      borderColor={'#bec3c9'}
      backgroundColor={colors.white}
      border={'1px'}
      _hover={{ bg: '#ebedf0' }}
      _active={{
        border: '2px',
        bg: '#dddfe2',
        transform: 'scale(0.98)',
        borderColor: '#bec3c9',
      }}
      _focus={{
        boxShadow: '0 0 10px 2px rgba(88, 144, 255, .75), 0 1px 1px rgba(0, 0, 0, .15)',
      }}
      {...(isSelected ? { ...selectedStyle } : {})}
      {...rest}
    >
      {children}
    </VStack>
  )
}

export default NodeWrapper
