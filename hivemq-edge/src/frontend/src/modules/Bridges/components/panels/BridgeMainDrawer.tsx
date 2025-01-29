import type { FC } from 'react'
import { useEffect } from 'react'
import {
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  Button,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Flex,
  Text,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'

import type { ApiError, Bridge } from '@/api/__generated__'
import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'

import ConnectionPanel from '../panels/ConnectionPanel.tsx'
import NamePanel from '../panels/NamePanel.tsx'
import OptionsPanel from '../panels/OptionsPanel.tsx'
import SubscriptionsPanel from '../panels/SubscriptionsPanel.tsx'
import SecurityPanel from '../panels/SecurityPanel.tsx'
import PersistencePanel from '../panels/PersistencePanel.tsx'
import { useBridgeSetup } from '../../hooks/useBridgeConfig.tsx'
import WebSocketPanel from '@/modules/Bridges/components/panels/WebSocketPanel.tsx'

interface BridgeMainDrawerProps {
  isNewBridge?: boolean
  isOpen: boolean
  isSubmitting: boolean
  error?: ApiError | null
  onClose: () => void
  onSubmit: (data: Bridge) => void
  onDelete?: () => void
}

const BridgeMainDrawer: FC<BridgeMainDrawerProps> = ({
  isNewBridge = false,
  isOpen,
  isSubmitting,
  onClose,
  onSubmit,
  onDelete,
}) => {
  const { t } = useTranslation()
  const { bridge } = useBridgeSetup()
  const hasPersistence = useGetCapability(Capability.id.MQTT_PERSISTENCE)
  const form = useForm<Bridge>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: bridge,
  })

  useEffect(() => {
    if (isOpen) form.reset(bridge)
  }, [bridge, isOpen, form])

  return (
    <>
      <Drawer
        variant="hivemq"
        closeOnOverlayClick={false}
        size="lg"
        isOpen={isOpen}
        placement="right"
        onClose={onClose}
      >
        <DrawerOverlay />
        <DrawerContent aria-label={t('bridge.drawer.label')}>
          <DrawerCloseButton />
          <DrawerHeader id="bridge-form-header" borderBottomWidth="1px">
            {isNewBridge ? t('bridge.drawer.title.create') : t('bridge.drawer.title.update')}
          </DrawerHeader>

          <DrawerBody>
            <form
              id="bridge-form"
              onSubmit={form.handleSubmit(onSubmit)}
              style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
            >
              <NamePanel form={form} isNewBridge={isNewBridge} />
              <Tabs>
                <TabList>
                  <Tab>{t('bridge.drawer.connection')}</Tab>
                  <Tab>{t('bridge.drawer.broker')}</Tab>
                  <Tab>{t('bridge.drawer.security')}</Tab>
                  <Tab>{t('bridge.drawer.websocket')}</Tab>
                  {hasPersistence && <Tab>{t('bridge.drawer.persistence')}</Tab>}
                </TabList>

                <TabPanels>
                  <TabPanel>
                    <ConnectionPanel form={form} />
                    <Tabs>
                      <TabList>
                        <Tab>{t('bridge.subscription.type', { context: 'local' })}</Tab>
                        <Tab>{t('bridge.subscription.type', { context: 'remote' })}</Tab>
                      </TabList>

                      <TabPanels>
                        <TabPanel px={0}>
                          <Text fontSize={{ md: 'sm' }}>{t('bridge.subscription.local.info')}</Text>
                          <SubscriptionsPanel form={form} type="localSubscriptions" />
                        </TabPanel>
                        <TabPanel px={0}>
                          <Text fontSize={{ md: 'sm' }}>{t('bridge.subscription.remote.info')}</Text>
                          <SubscriptionsPanel form={form} type="remoteSubscriptions" />
                        </TabPanel>
                      </TabPanels>
                    </Tabs>
                  </TabPanel>
                  <TabPanel>
                    <OptionsPanel form={form} />
                  </TabPanel>

                  <TabPanel>
                    <SecurityPanel form={form} />
                  </TabPanel>

                  <TabPanel>
                    <WebSocketPanel form={form} />
                  </TabPanel>

                  {hasPersistence && (
                    <TabPanel>
                      <PersistencePanel form={form} />
                    </TabPanel>
                  )}
                </TabPanels>
              </Tabs>
            </form>
          </DrawerBody>

          <DrawerFooter borderTopWidth="1px">
            {!isNewBridge && (
              <Button type="button" variant="danger" form="bridge-form" onClick={onDelete}>
                {t('bridge.action.delete')}
              </Button>
            )}
            <Flex flexGrow={1} justifyContent="flex-end">
              <Button
                isDisabled={!form.formState.isValid}
                isLoading={isSubmitting}
                variant="primary"
                type="submit"
                form="bridge-form"
              >
                {isNewBridge ? t('bridge.action.create') : t('bridge.action.update')}
              </Button>
            </Flex>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default BridgeMainDrawer
