import { UseFormReturn, FieldValues } from 'react-hook-form'

export type SubscriptionType = 'remoteSubscriptions' | 'localSubscriptions'

export interface GenericPanelType<T extends FieldValues> {
  isNew?: boolean
  form: UseFormReturn<T>
}

export interface AdapterType {
  adapterType?: string
}

export interface ProtocolFacetType {
  search: string | undefined
  filter: { key: string; value: string } | undefined
}

export interface UIGroup {
  id: string
  title: string
  children: string[]
}
