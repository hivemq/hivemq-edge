import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { BiShare, BsThreeDotsVertical, SiHomebridge } from 'react-icons/all'
import {
  Avatar,
  Box,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Flex,
  Heading,
  Button,
  TableContainer,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
} from '@chakra-ui/react'

import { Bridge, ConnectionStatus, ConnectionStatusTransitionCommand } from '@/api/__generated__'
import { useSetConnectionStatus } from '@/api/hooks/useGetBridges/useSetConnectionStatus.tsx'
import { useDeleteBridge } from '@/api/hooks/useGetBridges/useDeleteBridge.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import SubscriptionStats from '@/modules/Bridges/components/overview/SubscriptionStats.tsx'

import ConnectionSummary from './ConnectionSummary.tsx'

const BridgeOverview: FC<Bridge> = (props) => {
  const { t } = useTranslation()
  const { isLoading, isError, mutateAsync } = useSetConnectionStatus()
  const deleteBridge = useDeleteBridge()

  const { status } = props.bridgeRuntimeInformation?.connectionStatus || {}
  const isCtaDisabled = isError || !status
  const isCtaLoading =
    isLoading || status === ConnectionStatus.status.DISCONNECTING || status === ConnectionStatus.status.CONNECTING
  const isCtaConnected = status === ConnectionStatus.status.CONNECTED

  const deleteContent = () => (
    <CardBody>
      {deleteBridge.isLoading && (
        <Button isLoading loadingText={t('bridge.action.submitting')} colorScheme="teal" variant="outline">
          {t('bridge.action.delete')}
        </Button>
      )}
      {deleteBridge.isError && (
        <ErrorMessage
          type={deleteBridge.error?.message}
          message={(deleteBridge.error?.body as ProblemDetails)?.title}
        />
      )}
    </CardBody>
  )

  const viewContent = () => (
    <>
      <CardBody>
        <TableContainer tabIndex={0}>
          <ConnectionSummary {...props} />
          <SubscriptionStats local={props.localSubscriptions} remote={props.remoteSubscriptions} />
        </TableContainer>
      </CardBody>

      <CardFooter
        justify="space-between"
        flexWrap="wrap"
        gap={2}
        sx={{
          '& > button': {
            minW: '100px',
          },
        }}
      >
        <Button
          disabled={isCtaDisabled}
          isLoading={isCtaLoading}
          onClick={() =>
            mutateAsync({
              name: props.id as string,
              requestBody: {
                command: isCtaConnected
                  ? ConnectionStatusTransitionCommand.command.DISCONNECT
                  : ConnectionStatusTransitionCommand.command.CONNECT,
              },
            })
          }
          flex="1"
          colorScheme={isCtaConnected ? 'red' : 'green'}
          leftIcon={<BiShare />}
        >
          {isCtaConnected ? t('bridge.action.stop') : t('bridge.action.start')}
        </Button>
      </CardFooter>
    </>
  )

  return (
    <Card width="350px">
      <CardHeader>
        <Flex>
          <Flex flex="1" gap="1" alignItems="center" flexWrap="wrap">
            <Avatar icon={<SiHomebridge fontSize="1.5rem" />} />
            <Box>
              <Heading size="sm">{props.id}</Heading>
            </Box>
          </Flex>

          <Menu>
            {({ isOpen }) => (
              <>
                <MenuButton
                  aria-label={t('bridge.menu') as string}
                  isActive={isOpen}
                  as={Button}
                  variant="ghost"
                  colorScheme="gray"
                  rightIcon={<BsThreeDotsVertical />}
                />
                <MenuList>
                  <MenuItem isDisabled onClick={() => console.log('editing')}>
                    {t('bridge.action.edit')}
                  </MenuItem>
                  <MenuItem
                    onClick={() => {
                      deleteBridge.mutateAsync(props.id as string)
                    }}
                  >
                    {t('bridge.action.delete')}
                  </MenuItem>
                </MenuList>
              </>
            )}
          </Menu>
        </Flex>
      </CardHeader>
      {deleteBridge.isLoading || (deleteBridge.isError && deleteContent())}
      {deleteBridge.isIdle && viewContent()}
    </Card>
  )
}

export default BridgeOverview
