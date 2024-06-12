import { FC, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  HStack,
  Image,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import { ExportFormatDisplay, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { adapterExportFormats } from '@/modules/ProtocolAdapters/utils/export.utils.ts'

const ExportDrawer: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { adapterId } = useParams()
  const navigate = useNavigate()
  const { name, logo } = useGetAdapterInfo(adapterId)

  useEffect(() => {
    if (adapterId) {
      onOpen()
    }
  }, [adapterId, onOpen])

  const handleInstanceClose = () => {
    onClose()
    navigate('/protocol-adapters', { state: { protocolAdapterTabIndex: ProtocolAdapterTabIndex.PROTOCOLS } })
  }

  const listFormats = adapterExportFormats.map<ExportFormatDisplay>((exports) => {
    return {
      ...exports,
      label: t('protocolAdapter.export.formats.label', { context: exports.value }),
      description: t('protocolAdapter.export.formats.description', { context: exports.value }),
    }
  })

  return (
    <Drawer
      variant="hivemq"
      closeOnOverlayClick={false}
      size="md"
      isOpen={isOpen}
      placement="right"
      onClose={handleInstanceClose}
    >
      <DrawerOverlay />
      <DrawerContent aria-label={t('protocolAdapter.export.header')}>
        <DrawerCloseButton />
        <DrawerHeader id="adapter-discovery-header" borderBottomWidth="1px">
          <Text>{t('protocolAdapter.export.header')}</Text>
          <HStack>
            <Image boxSize="30px" objectFit="scale-down" src={logo} aria-label={name} />
            <Text fontSize="md" fontWeight="500">
              {name}
            </Text>
          </HStack>
        </DrawerHeader>
        <DrawerBody></DrawerBody>
        <DrawerFooter borderTopWidth="1px">
          <Flex flexGrow={1} justifyContent="flex-end">
            <Button variant="primary" type="submit" form="adapter-instance-form">
              {t('protocolAdapter.export.action.export')}
            </Button>
          </Flex>
        </DrawerFooter>{' '}
      </DrawerContent>
    </Drawer>
  )
}

export default ExportDrawer
