import { Bridge } from '@/api/__generated__'
import { UseFormReturn } from 'react-hook-form'

export type SubscriptionType = 'remoteSubscriptions' | 'localSubscriptions'

export interface BridgePanelType {
  isNewBridge?: boolean
  form: UseFormReturn<Bridge>
}

export interface BridgeSubscriptionsProps extends BridgePanelType {
  type: SubscriptionType
}
