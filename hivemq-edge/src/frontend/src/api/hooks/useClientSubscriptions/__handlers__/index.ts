import { BrokerClient } from '@/api/types/api-broker-client.ts'

export const mockClientSubscription: BrokerClient = {
  config: {
    id: 'my-first-client',
  },
  id: 'my-first-client',
  type: 'broker-client',
}

export const mockClientSubscriptionsList: BrokerClient[] = [mockClientSubscription]
