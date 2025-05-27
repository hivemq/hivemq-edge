import MiniMap from '@/components/react-flow/MiniMap.tsx'
import type { FC } from 'react'
import { useColorModeValue, useTheme } from '@chakra-ui/react'
import { DataHubNodeType } from '@datahub/types.ts'
import { getColor } from '@chakra-ui/theme-tools'

const DesignerMiniMap: FC = () => {
  const headerBackgroundColor = useColorModeValue('blue.100', 'blue.700')
  const headerPolicyBackgroundColor = useColorModeValue('orange.300', 'orange.500')
  const headerResourceBackgroundColor = useColorModeValue('pink.100', 'pink.700')
  const theme = useTheme()

  const backgroundColor = (type: string) =>
    getColor(
      theme,
      type === DataHubNodeType.DATA_POLICY || type === DataHubNodeType.BEHAVIOR_POLICY
        ? headerPolicyBackgroundColor
        : type === DataHubNodeType.SCHEMA || type === DataHubNodeType.FUNCTION
          ? headerResourceBackgroundColor
          : headerBackgroundColor
    )

  return (
    <MiniMap
      zoomable
      pannable
      nodeClassName={(node) => node.type || ''}
      nodeComponent={(miniMapNode) => {
        return (
          <rect
            x={miniMapNode.x}
            y={miniMapNode.y}
            width={miniMapNode.width}
            height={miniMapNode.height}
            fill={backgroundColor(miniMapNode.className)}
          />
        )
      }}
    />
  )
}

export default DesignerMiniMap
