import { useMemo } from 'react'
import type { UseQueryResult } from '@tanstack/react-query'

import type { ApiError, Combiner, DataCombining, ManagedAsset } from '@/api/__generated__'
import { useListCombiners } from '@/api/hooks/useCombiners/useListCombiners'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import type { ManagedAssetExtended } from '@/modules/Pulse/types.ts'

type PartialUseQueryResult = Pick<
  UseQueryResult<ManagedAssetExtended[], ApiError>,
  'isError' | 'error' | 'isSuccess' | 'isLoading' | 'data'
>

export const useCombinedAssetsAndCombiners = (): PartialUseQueryResult => {
  const managedAssets = useListManagedAssets()
  const combiners = useListCombiners()

  const isLoading = managedAssets.isLoading || combiners.isLoading
  const isError = managedAssets.isError || combiners.isError
  const error = managedAssets.error || combiners.error
  const isSuccess = managedAssets.isSuccess && combiners.isSuccess

  const data = useMemo<ManagedAssetExtended[] | undefined>(() => {
    if (!isSuccess) return undefined

    const assets: ManagedAsset[] = managedAssets.data?.items ?? []
    const combinersList: Combiner[] = combiners.data?.items ?? []

    const allMappings: DataCombining[] = combinersList.flatMap((combiner) => combiner.mappings.items ?? [])
    console.log('XXXXXX', allMappings)

    return assets.map<ManagedAssetExtended>((asset) => {
      const mapping = asset.mapping?.mappingId ? allMappings.find((m) => m.id === asset.mapping?.mappingId) : undefined

      return {
        asset,
        mapping,
      }
    })
  }, [combiners.data?.items, isSuccess, managedAssets.data?.items])

  return {
    isLoading,
    isError,
    error,
    isSuccess,
    data,
  }
}
