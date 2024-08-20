import { BrokerClient, BrokerClientSubscription } from '@/api/types/api-broker-client.ts'
import maxQoS = BrokerClientSubscription.maxQoS

export const mockClientSubscription: BrokerClient = {
  config: {
    id: 'my-first-client',
    subscriptions: [{ destination: 'test/topic/1', maxQoS: maxQoS._0 }],
  },
  id: 'my-first-client',
  type: 'broker-client',
}

export const mockClientSubscriptionsList: BrokerClient[] = [
  mockClientSubscription,
  {
    config: {
      id: 'my-first-client2',
    },
    id: 'my-first-client2',
    type: 'broker-client',
  },
]
