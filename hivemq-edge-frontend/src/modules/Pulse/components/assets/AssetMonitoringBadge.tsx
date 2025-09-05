import type { FC } from 'react'
import { useMemo } from 'react'
import { Badge } from '@chakra-ui/react'

import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

const AssetMonitoringBadge: FC = () => {
  const { data: assets, isLoading } = useListManagedAssets()

  const unattendedAssets = useMemo(() => {
    if (!assets?.items) return undefined
    return assets.items.filter(
      (asset) =>
        asset.mapping.status === AssetMapping.status.UNMAPPED ||
        asset.mapping.status === AssetMapping.status.REQUIRES_REMAPPING
    )?.length
  }, [assets?.items])

  if (isLoading || !unattendedAssets) return <LoaderSpinner boxSize={4} />

  return <Badge>{unattendedAssets}</Badge>
}

export default AssetMonitoringBadge
