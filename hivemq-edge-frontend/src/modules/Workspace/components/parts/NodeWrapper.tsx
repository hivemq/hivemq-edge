import type { FC } from 'react'
import { type CardProps, Card, useTheme } from '@chakra-ui/react'

import type { NodeStatusModel } from '@/modules/Workspace/types/status.types'
import { getStatusColor } from '@/modules/Workspace/utils/status-utils'

interface NodeWrapperProps extends CardProps {
  isSelected?: boolean
  statusModel?: NodeStatusModel
}

const NodeWrapper: FC<NodeWrapperProps> = ({ children, isSelected = false, statusModel, ...rest }) => {
  const theme = useTheme()
  const statusColor = getStatusColor(theme, statusModel)

  const selectedStyle: Partial<CardProps> = {
    // Use status color for selection shadow instead of default blue
    boxShadow: `0 0 0 3px ${statusColor}40, 0 0 10px 2px ${statusColor}60, 0 1px 1px rgba(0, 0, 0, 0.15)`,
  }

  return (
    <Card
      variant="elevated"
      {...(isSelected ? { ...selectedStyle } : {})}
      p={6}
      rounded="md"
      borderWidth={1}
      // Set CSS custom property for focus-visible shadow
      style={{
        ...rest.style,
        ['--node-status-color' as string]: statusColor,
      }}
      {...rest}
    >
      {children}
    </Card>
  )
}

export default NodeWrapper
