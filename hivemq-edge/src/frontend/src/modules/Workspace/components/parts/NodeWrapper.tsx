import { FC } from 'react'
import { type BoxProps, Card } from '@chakra-ui/react'

interface NodeWrapperProps extends BoxProps {
  isSelected?: boolean
}

const NodeWrapper: FC<NodeWrapperProps> = ({ children, isSelected = false, ...rest }) => {
  // const {  ...props } = useTheme()

  const selectedStyle: Partial<BoxProps> = {
    // boxShadow: 'dark-lg',
    // bg: '#dddfe2',
    boxShadow: 'var(--chakra-shadows-outline)',
  }

  return (
    <Card
      variant={'elevated'}
      // colorScheme={'green'}
      p={6}
      rounded={'md'}
      border={`1px`}
      // borderColor={'#bec3c9'}
      // backgroundColor={colors.white}
      // _hover={{ bg: '#ebedf0' }}
      {...(isSelected ? { ...selectedStyle } : {})}
      {...rest}
    >
      <>{children}</>
    </Card>
  )
}

export default NodeWrapper
