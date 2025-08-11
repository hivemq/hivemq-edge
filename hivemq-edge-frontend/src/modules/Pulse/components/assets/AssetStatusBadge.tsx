import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, SkeletonCircle } from '@chakra-ui/react'

import { AssetMapping } from '@/api/__generated__'

const statusMapping: Record<AssetMapping.status, { text: string; color: string }> = {
  [AssetMapping.status.UNMAPPED]: { text: 'UNMAPPED', color: 'status.unknown' },
  [AssetMapping.status.DRAFT]: { text: 'DRAFT', color: 'status.disconnected' },
  [AssetMapping.status.STREAMING]: { text: 'STREAMING', color: 'status.connected' },
  [AssetMapping.status.REQUIRES_REMAPPING]: { text: 'REQUIRES_REMAPPING', color: 'status.error' },
}

interface AssetStatusBadgeProps {
  status: AssetMapping.status
  skeleton?: boolean
}

const AssetStatusBadge: FC<AssetStatusBadgeProps> = ({ status, skeleton = false }) => {
  const { t } = useTranslation()

  const mapping = statusMapping[status || AssetMapping.status.UNMAPPED]

  if (skeleton)
    return (
      <SkeletonCircle
        size="8"
        startColor={`${mapping.color}.300`}
        endColor={`${mapping.color}.500`}
        aria-label={mapping.text}
      />
    )

  return (
    <Badge
      variant="subtle"
      colorScheme={mapping.color}
      borderRadius={15}
      data-testid="asset-status"
      data-status={status}
    >
      {t('pulse.assets.status', { context: mapping.text })}
    </Badge>
  )
}

export default AssetStatusBadge
