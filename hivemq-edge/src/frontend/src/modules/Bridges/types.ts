import { UseFormReturn } from 'react-hook-form'
import { Bridge } from '@/api/__generated__'

export type SubscriptionType = 'remoteSubscriptions' | 'localSubscriptions'

export interface BridgePanelType {
  isNewBridge?: boolean
  form: UseFormReturn<Bridge>
}

export interface BridgeSubscriptionsProps extends BridgePanelType {
  type: SubscriptionType
}
