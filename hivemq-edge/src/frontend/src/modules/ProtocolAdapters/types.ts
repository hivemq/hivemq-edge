import { UseFormReturn, FieldValues } from 'react-hook-form'
import { Adapter, ProtocolAdapter } from '@/api/__generated__'

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

export interface UITab {
  id: string
  title: string
  properties: string[]
}

export enum ProtocolAdapterTabIndex {
  protocols = 0,
  adapters = 1,
}

export interface AdapterNavigateState {
  protocolAdapterTabIndex: ProtocolAdapterTabIndex
  protocolAdapterType?: string
  selectedActiveAdapter?: {
    isNew: boolean
    isOpen: boolean
    adapterId: string
  }
}

export type AdapterConfig = NonNullable<Adapter['config']>
