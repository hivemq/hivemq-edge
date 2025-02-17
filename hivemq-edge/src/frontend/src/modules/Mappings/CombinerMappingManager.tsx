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

import type { Combiner } from '@/api/__generated__'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'
import type { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { MappingType } from './types'

const CombinerMappingManager: FC = () => {
  const { t } = useTranslation()
  const [isExpanded, setExpanded] = useBoolean(true)
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { nodeId } = useParams()
  const { nodes } = useWorkspaceStore()

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === nodeId) as Node<Combiner> | undefined
  }, [nodeId, nodes])

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const [showNativeWidgets, setShowNativeWidgets] = useBoolean()

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'lg'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
        <DrawerHeader>
          <Text>{t('protocolAdapter.mapping.manager.header', { context: MappingType.COMBINING })}</Text>
          <NodeNameCard
            name={selectedNode?.data.id}
            type={selectedNode?.type as NodeTypes}
            // icon={adapterProtocol?.logoUrl}
            // description={adapterProtocol?.name}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}></DrawerBody>
        TODO
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

export default CombinerMappingManager
