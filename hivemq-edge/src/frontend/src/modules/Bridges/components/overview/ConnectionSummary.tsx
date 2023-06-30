import { FC } from 'react'
import { Bridge } from '@/api/__generated__'
import { Box, Table, Tbody, Td, Tooltip, Tr } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { formatHost } from '../../utils/formatters.tsx'

type BridgeConnection = Pick<
  Bridge,
  'host' | 'port' | 'sessionExpiry' | 'clientId' | 'localSubscriptions' | 'remoteSubscriptions'
>

const ConnectionSummary: FC<BridgeConnection> = ({ host, port, clientId, localSubscriptions, remoteSubscriptions }) => {
  const { t } = useTranslation()

  return (
    <Table variant="simple" size="sm">
      <Tbody>
        <Tr>
          <Td px={0} colSpan={2}>
            <Tooltip label={host} hasArrow placement="top">
              <Box overflow={'hidden'} textOverflow={'ellipsis'} fontWeight={'bold'}>
                {formatHost(host, 20)}
              </Box>
            </Tooltip>
          </Td>
        </Tr>
        <Tr>
          <Td px={0}>{t('bridge.connection.port')}</Td>
          <Td px={0} isNumeric>
            {port}
          </Td>
        </Tr>
        <Tr>
          <Td px={0}>{t('bridge.connection.clientId')}</Td>
          <Td px={0} isNumeric>
            {clientId}
          </Td>
        </Tr>
        <Tr>
          <Td px={0}>{t('bridge.subscription.type', { context: 'local', count: localSubscriptions?.length || 0 })}</Td>
          <Td px={0} isNumeric>
            {localSubscriptions?.length || 0}
          </Td>
        </Tr>
        <Tr>
          <Td px={0}>
            {t('bridge.subscription.type', { context: 'remote', count: remoteSubscriptions?.length || 0 })}
          </Td>
          <Td px={0} isNumeric>
            {remoteSubscriptions?.length || 0}
          </Td>
        </Tr>
      </Tbody>
    </Table>
  )
}

export default ConnectionSummary
