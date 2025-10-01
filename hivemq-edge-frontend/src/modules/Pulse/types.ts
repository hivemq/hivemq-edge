import type { DataCombining, ManagedAsset } from '@/api/__generated__'

export interface ManagedAssetExtended {
  asset: ManagedAsset
  // combinerId?: Combiner
  combinerId?: string
  mapping?: DataCombining
}
