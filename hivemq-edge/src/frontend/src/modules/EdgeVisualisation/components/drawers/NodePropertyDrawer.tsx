import { FC, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useNodes } from 'reactflow'
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
  Text,
  useDisclosure,
  VStack,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'

import { Adapter } from '@/api/__generated__'
import Metrics from '@/modules/Welcome/components/Metrics.tsx'
import { ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'

import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'

const NodePropertyDrawer: FC = () => {
  const { t } = useTranslation()
  const nodes = useNodes()
  const { nodeId } = useParams()
  const selected = nodes.find((e) => e.id === nodeId)
  const navigate = useNavigate()

  const { isOpen, onOpen, onClose } = useDisclosure()

  useEffect(() => {
    if (!nodes.length) return
    if (!selected || !nodeId) {
      navigate('/edge-flow', { replace: true })
      return
    }
    onOpen()
  }, [navigate, nodeId, nodes.length, onOpen, selected])

  const handleClose = () => {
    onClose()
    navigate('/edge-flow')
  }

  if (!selected) return null

  return (
    <Drawer isOpen={isOpen} placement="right" size={'md'} onClose={handleClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text>{t('workspace.observability.adapter.header')}</Text>
        </DrawerHeader>
        <DrawerBody>
          <VStack gap={4} alignItems={'stretch'}>
            <Metrics initMetrics={getDefaultMetricsFor(selected)} />
          </VStack>
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px">
          <Flex flexGrow={1} justifyContent={'flex-start'}>
            <Button
              data-testid={'protocol-create-adapter'}
              variant={'outline'}
              size={'sm'}
              rightIcon={<EditIcon />}
              onClick={() =>
                navigate('/protocol-adapters', {
                  state: {
                    protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
                    selectedAdapter: { isNew: false, adapterId: (selected?.data as Adapter).id },
                  },
                })
              }
            >
              {t('workspace.observability.adapter.modify')}
            </Button>
          </Flex>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default NodePropertyDrawer
