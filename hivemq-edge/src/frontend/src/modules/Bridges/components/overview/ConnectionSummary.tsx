import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Tooltip, chakra as Chakra, VisuallyHidden, Badge } from '@chakra-ui/react'

import { Bridge } from '@/api/__generated__'
import { formatHost } from '../../utils/formatters.tsx'

type BridgeConnection = Pick<
  Bridge,
  'host' | 'port' | 'sessionExpiry' | 'clientId' | 'localSubscriptions' | 'remoteSubscriptions'
>

const ConnectionSummary: FC<BridgeConnection> = ({ host, port, clientId, localSubscriptions, remoteSubscriptions }) => {
  const { t } = useTranslation()

  return (
    <Chakra.dl display={'grid'} gridTemplateColumns={'repeat(2, minmax(0px, 1fr))'} columnGap={4} alignItems={'center'}>
      <Chakra.dt>
        <VisuallyHidden>{t('bridge.connection.host')}</VisuallyHidden>
      </Chakra.dt>
      <Chakra.dd gridColumn={'1/ span 2'}>
        <Tooltip label={host} hasArrow placement="top">
          <Box overflow={'hidden'} textOverflow={'ellipsis'} fontWeight={'bold'}>
            {formatHost(host, 20)}
          </Box>
        </Tooltip>
      </Chakra.dd>
      <Chakra.dt>{t('bridge.connection.port')}</Chakra.dt>
      <Chakra.dd>{port}</Chakra.dd>
      <Chakra.dt>{t('bridge.connection.clientId')}</Chakra.dt>
      <Chakra.dd>{clientId}</Chakra.dd>
      <Chakra.dt>
        {t('bridge.subscription.type', { context: 'local', count: localSubscriptions?.length || 0 })}
      </Chakra.dt>
      <Chakra.dd>
        <Badge variant="subtle">{localSubscriptions?.length || 0}</Badge>
      </Chakra.dd>
      <Chakra.dt>
        {t('bridge.subscription.type', { context: 'remote', count: remoteSubscriptions?.length || 0 })}
      </Chakra.dt>
      <Chakra.dd>
        <Badge variant="subtle">{remoteSubscriptions?.length || 0} </Badge>
      </Chakra.dd>
    </Chakra.dl>
  )
}

export default ConnectionSummary
