import { UseFormReturn, FieldValues } from 'react-hook-form'
import { ProtocolAdapter } from '@/api/__generated__'

export type SubscriptionType = 'remoteSubscriptions' | 'localSubscriptions'

export interface GenericPanelType<T extends FieldValues> {
  isNew?: boolean
  form: UseFormReturn<T>
}

export interface AdapterType {
  adapterType?: string
}

export interface ProtocolFacetType {
  search?: string | null
  filter?: { key: keyof ProtocolAdapter; value: string } | null
}

export interface UIGroup {
  id: string
  title: string
  children: string[]
}
