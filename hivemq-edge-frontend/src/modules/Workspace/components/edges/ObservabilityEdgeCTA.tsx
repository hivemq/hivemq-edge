import type { CSSProperties, FC } from 'react'
import { LuBarChartBig } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, Icon, useColorMode } from '@chakra-ui/react'

import IconButton from '@/components/Chakra/IconButton.tsx'

interface ObservabilityEdgeCtaProps {
  source: string
  style?: CSSProperties
  onClick?: () => void
}

const ObservabilityEdgeCTA: FC<ObservabilityEdgeCtaProps> = ({ source, style, onClick }) => {
  const { t } = useTranslation()
  const { colorMode } = useColorMode()

  return (
    <ButtonGroup>
      <IconButton
        data-testid="observability-panel-trigger"
        aria-label={t('workspace.observability.aria-label', { device: source })}
        variant={colorMode === 'light' ? 'outline' : 'solid'}
        icon={<Icon as={LuBarChartBig} boxSize={6} />}
        backgroundColor={colorMode === 'light' ? 'white' : 'gray.700'}
        color={style?.stroke}
        onClick={onClick}
        borderRadius={25}
      />
    </ButtonGroup>
  )
}

export default ObservabilityEdgeCTA
