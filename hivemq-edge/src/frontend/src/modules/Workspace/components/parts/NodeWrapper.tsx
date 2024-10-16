import { FC } from 'react'
import { type CardProps, Card } from '@chakra-ui/react'

interface NodeWrapperProps extends CardProps {
  isSelected?: boolean
}

const NodeWrapper: FC<NodeWrapperProps> = ({ children, isSelected = false, ...rest }) => {
  const selectedStyle: Partial<CardProps> = {
    boxShadow: 'var(--chakra-shadows-outline)',
  }

  return (
    <Card variant="elevated" {...(isSelected ? { ...selectedStyle } : {})} p={6} rounded="md" borderWidth={1} {...rest}>
      {children}
    </Card>
  )
}

export default NodeWrapper
