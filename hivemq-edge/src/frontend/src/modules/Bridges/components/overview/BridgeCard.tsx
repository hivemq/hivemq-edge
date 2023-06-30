import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Heading,
  Image,
  Stack,
  IconButton,
  Box,
  HStack,
  Text,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'

import { Bridge } from '@/api/__generated__'
import BridgeLogo from '@/assets/app/bridges.svg'

import ConnectionSummary from './ConnectionSummary.tsx'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'

const BridgeCard: FC<Bridge> = (props) => {
  const { t } = useTranslation()
  const navigate = useNavigate()

  // const { isFetching } = useGetBridgeConnectionStatus(props.id)
  const { isFetching, data: connections } = useGetBridgesStatus()

  const data = useMemo(
    () => connections?.items?.find((connection) => connection.id === props.id && connection.type === 'bridge'),
    [connections, props.id]
  )

  return (
    <Card direction={{ base: 'column', md: 'column' }} mt={0} overflow="hidden" variant="outline">
      <HStack>
        <Image boxSize="100px" src={BridgeLogo} alt={t('bridge.title') as string} />
        <Stack>
          <CardHeader display={'flex'} pb={0} w={300}>
            <Heading size="md" flexGrow={1} m={'auto'}>
              {props.id}
            </Heading>

            <IconButton
              variant="ghost"
              aria-label={t('bridge.subscription.edit')}
              icon={<EditIcon />}
              onClick={() => navigate(`/mqtt-bridges/${props.id}`)}
            />
          </CardHeader>
          <CardBody py={0}>
            <ConnectionSummary {...props} />
          </CardBody>
        </Stack>
      </HStack>
      <CardFooter flexDirection={'row'} gap={2}>
        <Box>
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
          <ConnectionStatusBadge status={data?.status} />
        </Box>
      </CardFooter>
    </Card>
  )
}

export default BridgeCard
