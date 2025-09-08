import type { FC } from 'react'
import { useMemo } from 'react'
import { Badge } from '@chakra-ui/react'

import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

const BADGE_ERROR_CONTENT = '?'

const AssetMonitoringBadge: FC = () => {
  const { data: assets, isLoading, error } = useListManagedAssets()

  const unattendedAssets = useMemo(() => {
    if (!assets?.items || error) return undefined
    return assets.items.filter(
      (asset) =>
        asset.mapping.status === AssetMapping.status.UNMAPPED ||
        asset.mapping.status === AssetMapping.status.REQUIRES_REMAPPING
    )?.length
  }, [assets?.items, error])

  if (isLoading) return <LoaderSpinner boxSize={4} />

  return (
    <Badge data-testid="asset-monitoring-unattended" colorScheme={unattendedAssets ? undefined : 'red'}>
      {unattendedAssets || BADGE_ERROR_CONTENT}
    </Badge>
  )
}

export default AssetMonitoringBadge
