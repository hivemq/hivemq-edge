import type { FC } from 'react'
import type { MiniMapProps } from 'reactflow'
import { MiniMap as ReactFlowMinimap } from 'reactflow'
import { useTheme, useColorMode } from '@chakra-ui/react'

const Minimap: FC<MiniMapProps> = (props) => {
  const { colors } = useTheme()
  const { colorMode } = useColorMode()

  return (
    <ReactFlowMinimap
      {...props}
      nodeStrokeWidth={1}
      nodeColor={colorMode === 'light' ? colors.gray[300] : colors.gray[700]}
      maskColor={colorMode === 'light' ? colors.gray[200] : colors.gray[700]}
      style={{ backgroundColor: colorMode === 'light' ? colors['white'] : colors.gray[500] }}
    />
  )
}

export default Minimap
