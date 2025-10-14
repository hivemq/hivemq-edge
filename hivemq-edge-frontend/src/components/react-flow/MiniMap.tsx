import type { FC } from 'react'
import type { MiniMapProps } from '@xyflow/react'
import { MiniMap as ReactFlowMiniMap } from '@xyflow/react'
import type { As } from '@chakra-ui/react'
import { Box } from '@chakra-ui/react'
import { chakra } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

const MiniMapChakra = chakra<As, MiniMapProps>(ReactFlowMiniMap)

const MiniMap: FC<MiniMapProps> = (props) => {
  const { t } = useTranslation()
  return (
    <Box role="group" aria-label={t('workspace.canvas.toolbar.minimap')}>
      <MiniMapChakra
        {...props}
        role="group"
        aria-label={t('workspace.canvas.toolbar.controls')}
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
    </Box>
  )
}

export default MiniMap
