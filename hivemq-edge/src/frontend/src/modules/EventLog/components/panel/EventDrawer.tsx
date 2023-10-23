import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Drawer, DrawerBody, DrawerHeader, DrawerContent, DrawerCloseButton } from '@chakra-ui/react'

import { Event } from '@/api/__generated__'

interface BridgeMainDrawerProps {
  event: Event | undefined
  isOpen: boolean

  onClose: () => void
}

const EventDrawer: FC<BridgeMainDrawerProps> = ({ event, isOpen, onClose }) => {
  const { t } = useTranslation()

  return (
    <>
      <Drawer
        variant={'hivemq'}
        closeOnOverlayClick={true}
        size={'lg'}
        isOpen={isOpen}
        placement="right"
        onClose={onClose}
      >
        <DrawerContent aria-label={t('bridge.drawer.label') as string}>
          <DrawerCloseButton />
          <DrawerHeader id={'bridge-form-header'}>{t('eventLog.panel.title')}</DrawerHeader>

          <DrawerBody></DrawerBody>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default EventDrawer
