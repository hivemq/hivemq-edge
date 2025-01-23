import { type FC, useEffect, useMemo } from 'react'
import type { Node } from 'reactflow'
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
  FormControl,
  FormLabel,
  Switch,
  Text,
  useBoolean,
  useDisclosure,
} from '@chakra-ui/react'

import config from '@/config'

import type { Adapter } from '@/api/__generated__'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import MappingForm from '@/modules/Mappings/components/MappingForm.tsx'
import { useNorthboundMappingManager } from '@/modules/Mappings/hooks/useNorthboundMappingManager.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { useSouthboundMappingManager } from '@/modules/Mappings/hooks/useSouthboundMappingManager.ts'
import { MappingType } from './types'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'

interface AdapterMappingManagerProps {
  type: MappingType
}

// TODO[NVL] Ensure layout fully responsive
const AdapterMappingManager: FC<AdapterMappingManagerProps> = ({ type }) => {
  const { t } = useTranslation()
  const [isExpanded, setExpanded] = useBoolean(true)
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { nodeId } = useParams()
  const { nodes } = useWorkspaceStore()

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === nodeId && node.type === NodeTypes.ADAPTER_NODE) as Node<Adapter> | undefined
  }, [nodeId, nodes])

  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol =
    selectedNode?.type === NodeTypes.ADAPTER_NODE
      ? protocols?.items?.find((e) => e.id === (selectedNode as Node<Adapter>).data.type)
      : undefined

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const adapterId = selectedNode?.data.id
  const manager = type === MappingType.NORTHBOUND ? useNorthboundMappingManager : useSouthboundMappingManager

  const [showNativeWidgets, setShowNativeWidgets] = useBoolean()

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'lg'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
        <DrawerHeader>
          <Text>{t('protocolAdapter.mapping.manager.header', { context: type })}</Text>
          <NodeNameCard
            name={selectedNode?.data.id}
            type={selectedNode?.type as NodeTypes}
            icon={adapterProtocol?.logoUrl}
            description={adapterProtocol?.name}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {!adapterId && <ErrorMessage message={t('protocolAdapter.error.loading')} />}
          {adapterId && (
            <MappingForm
              adapterId={adapterId}
              adapterType={selectedNode?.data.type}
              onSubmit={handleClose}
              useManager={manager}
              type={type}
              showNativeWidgets={showNativeWidgets}
            />
          )}
        </DrawerBody>
        <DrawerFooter>
          {config.environment === 'development' && (
            <FormControl display="flex" alignItems="center">
              <FormLabel htmlFor="email-alerts" mb="0">
                {t('modals.native')}
              </FormLabel>
              <Switch id="email-alerts" isChecked={showNativeWidgets} onChange={setShowNativeWidgets.toggle} />
            </FormControl>
          )}
          <Button variant="primary" type="submit" form="adapter-mapping-form">
            {t('protocolAdapter.mapping.actions.submit')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterMappingManager
