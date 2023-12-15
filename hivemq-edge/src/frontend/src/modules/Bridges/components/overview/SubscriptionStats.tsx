import { BridgeSubscription } from '@/api/__generated__'
import { Flex, Stat, StatHelpText, StatLabel, StatNumber } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'

interface SubscriptionStatsProps {
  local?: BridgeSubscription[]
  remote?: BridgeSubscription[]
}

const SubscriptionStats: FC<SubscriptionStatsProps> = ({ local, remote }) => {
  const { t } = useTranslation()

  return (
    <Flex gap={3}>
      {local && (
        <Stat>
          <StatLabel>{t('bridge.subscription.type', { context: 'local' })}</StatLabel>
          <StatNumber>{local.length}</StatNumber>
          <StatHelpText>{t('bridge.subscription.description', { count: local.length })}</StatHelpText>
        </Stat>
      )}
      {remote && (
        <Stat>
          <StatLabel>{t('bridge.subscription.type', { context: 'remote' })}</StatLabel>
          <StatNumber>{remote.length}</StatNumber>
          <StatHelpText>{t('bridge.subscription.description', { count: remote.length })}</StatHelpText>
        </Stat>
      )}
    </Flex>
  )
}

export default SubscriptionStats
