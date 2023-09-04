import { Node } from 'reactflow'

import { Adapter } from '@/api/__generated__'
import { DateTime } from 'luxon'

export const groupingAttributes = [
  { name: 'Type', key: (d: Node<Adapter>) => d.data.type },
  { name: 'Subscription count', key: (d: Node<Adapter>) => !!d.data.config?.subscriptions.length },
  {
    name: 'Runtime duration',
    key: (d: Node<Adapter>) => {
      const gg = d.data.adapterRuntimeInformation?.lastStartedAttemptTime

      if (!gg) return 0
      const ss = DateTime.fromISO(gg).toMillis()
      return ss % 3
    },
  },
]
