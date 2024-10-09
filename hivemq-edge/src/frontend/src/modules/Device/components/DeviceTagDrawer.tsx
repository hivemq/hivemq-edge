import { FC } from 'react'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { LuFileCog } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'
import DeviceTagForm from '@/modules/Device/components/DeviceTagForm.tsx'
import { Adapter } from '@/api/__generated__'

interface DeviceTagDrawerProps {
  adapter?: Adapter
  isDisabled?: boolean
}

const DeviceTagDrawer: FC<DeviceTagDrawerProps> = ({ adapter, isDisabled = false }) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()

  return (
    <>
      <IconButton
        variant="primary"
        aria-label={t('device.drawer.tagList.cta.edit')}
        icon={<LuFileCog />}
        isDisabled={isDisabled}
        onClick={onOpen}
      />
      <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} closeOnOverlayClick={false}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>
            <Text> {t('device.drawer.tagEditor.title')}</Text>
          </DrawerHeader>

          <DrawerBody>
            <DeviceTagForm adapterId={adapter?.id as string} adapterType={adapter?.type} />
          </DrawerBody>

          <DrawerFooter>
            <Button variant="primary" type="submit" form="adapter-instance-form">
              {t('unifiedNamespace.submit.label')}
            </Button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default DeviceTagDrawer
