import { FC } from 'react'
import { MiniMap as ReactFlowMinimap } from 'reactflow'
import { useTheme, useColorMode } from '@chakra-ui/react'

const Minimap: FC = () => {
  const { colors } = useTheme()
  const { colorMode } = useColorMode()

  return (
    <ReactFlowMinimap
      nodeStrokeWidth={1}
      nodeColor={colorMode === 'light' ? colors.gray[300] : colors.gray[700]}
      maskColor={colorMode === 'light' ? colors.gray[200] : colors.gray[700]}
      style={{ backgroundColor: colorMode === 'light' ? colors['white'] : colors.gray[500] }}
    />
  )
}

export default Minimap
