import { FC } from 'react'
import { Table, TableCaption, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { BridgeSubscription } from '@/api/__generated__'

interface SubscriptionsProps {
  type: 'local' | 'remote'
  subscriptions: Array<BridgeSubscription>
}

const SubscriptionSummary: FC<SubscriptionsProps> = ({ subscriptions, type }) => {
  const { t } = useTranslation()

  return (
    <Table variant="simple" size="sm">
      <TableCaption placement={'top'}>{t('bridge.subscription.type', { context: type })}</TableCaption>
      <Thead>
        <Tr>
          <Th>{t('bridge.subscription.filters.label')}</Th>
          <Th>{t('bridge.subscription.destination.label')}</Th>
          <Th>{t('bridge.subscription.maxQoS.label')}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {subscriptions.map((sub, index) => (
          <Tr key={`${sub.destination}-${index}`}>
            <Td>{sub.filters}</Td>
            <Td>{sub.destination}</Td>
            <Td>{sub.maxQoS}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

export default SubscriptionSummary
