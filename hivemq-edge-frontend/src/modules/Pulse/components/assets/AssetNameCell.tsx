import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Text } from '@chakra-ui/react'

import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

interface AssetNameCellProps {
  assetId?: string
  showDescription?: boolean
}

const AssetNameCell: FC<AssetNameCellProps> = ({ assetId, showDescription = false }) => {
  const { t } = useTranslation()
  const { data, isLoading } = useListManagedAssets()

  const asset = useMemo(() => {
    if (!data?.items) return undefined

    return data.items.find((e) => e.id === assetId)
  }, [assetId, data?.items])

  if (isLoading) return <LoaderSpinner />
  if (!assetId)
    return (
      <Text data-testid="asset-error" whiteSpace="nowrap">
        {t('pulse.assets.listing.sources.unset')}
      </Text>
    )

  if (!asset)
    return (
      <Text data-testid="asset-error" whiteSpace="nowrap">
        {t('pulse.assets.listing.sources.notFound')}
      </Text>
    )

  return (
    <>
      <Text data-testid="asset-name" whiteSpace="nowrap">
        {asset.name}
      </Text>
      {showDescription && (
        <Text data-testid="asset-description" noOfLines={1} fontStyle="italic">
          {asset.description}
        </Text>
      )}
    </>
  )
}

export default AssetNameCell
