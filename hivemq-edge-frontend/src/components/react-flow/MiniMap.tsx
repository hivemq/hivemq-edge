import type { FC } from 'react'
import type { MiniMapProps } from '@xyflow/react'
import { MiniMap as ReactFlowMiniMap } from '@xyflow/react'
import type { As } from '@chakra-ui/react'
import { chakra } from '@chakra-ui/react'

const MiniMapChakra = chakra<As, MiniMapProps>(ReactFlowMiniMap)

const MiniMap: FC<MiniMapProps> = (props) => {
  return (
    <MiniMapChakra
      {...props}
      sx={{
        borderColor: 'var(--chakra-colors-chakra-border-color)',
        borderWidth: 1,
        _dark: {
          backgroundColor: 'var(--chakra-colors-gray-700)',
        },
        borderRadius: 'var(--chakra-radii-md)',
        backgroundColor: 'var(--chakra-colors-chakra-body-bg)',
        boxShadow: 'var(--chakra-shadows-xl)',
      }}
    />
  )
}

export default MiniMap
