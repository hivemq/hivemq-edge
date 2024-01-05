import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Box,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Flex,
  Heading,
  HStack,
  Image,
  Skeleton,
  Text,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'

import BridgeLogo from '@/assets/app/bridges.svg'

import { Bridge } from '@/api/__generated__'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.tsx'
import { DeviceTypes } from '@/api/types/api-devices.ts'

import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'

import ConnectionSummary from './ConnectionSummary.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

interface BridgeCardProps extends Bridge {
  isLoading?: boolean
  onNavigate?: (route: string) => void
  role?: string
}

const BridgeCard: FC<BridgeCardProps> = ({ isLoading, onNavigate, role, ...props }) => {
  const { t } = useTranslation()

  // const { isFetching } = useGetBridgeConnectionStatus(props.id)
  const { isFetching, data: connections } = useGetBridgesStatus()

  const status = useMemo(
    () => connections?.items?.find((connection) => connection.id === props.id && connection.type === 'bridge'),
    [connections, props.id]
  )

  return (
    <Card overflow="hidden" aria-labelledby={'bridge-name'} role={role}>
      <CardHeader>
        <Skeleton isLoaded={!isLoading} display={'flex'}>
          <Heading size="md" flex={1} m={'auto'} data-testid={'bridge-name'} id={'bridge-name'}>
            {props.id}
          </Heading>
          <Box>
            <IconButton
              aria-label={t('bridge.action.edit')}
              icon={<EditIcon />}
              onClick={() => onNavigate?.(`/mqtt-bridges/${props.id}`)}
            />
          </Box>
        </Skeleton>
      </CardHeader>
      <CardBody py={0}>
        <HStack>
          <Skeleton isLoaded={!isLoading}>
            <Image boxSize="100px" src={BridgeLogo} alt={t('bridge.title') as string} />
          </Skeleton>
          <Skeleton isLoaded={!isLoading}>
            <ConnectionSummary {...props} />
          </Skeleton>
        </HStack>
      </CardBody>
      <CardFooter>
        <Skeleton isLoaded={!isLoading} as={Flex} w={'100%'}>
          <Box flex={1}>
            <span
              style={{
                display: 'inline-block',
                marginLeft: '.2rem',
                marginRight: '.2rem',
                width: `.5rem`,
                height: '.5rem',
                background: isFetching ? 'lightgrey' : 'transparent',
                transition: !isFetching ? 'all .3s ease' : 'none',
                borderRadius: '100%',
              }}
            />
            <Text as={'span'} mr={2}>
              {t('bridge.status.label')}
            </Text>
            <ConnectionStatusBadge status={status} />
          </Box>
          <Flex justifyContent={'flex-end'} role={'toolbar'}>
            <ConnectionController type={DeviceTypes.BRIDGE} id={props.id} status={props.status} />
          </Flex>
        </Skeleton>
      </CardFooter>
    </Card>
  )
}

export default BridgeCard
