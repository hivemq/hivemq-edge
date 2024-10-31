import { type FC, useEffect, useMemo } from 'react'
import { Node } from 'reactflow'
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
  Text,
  useBoolean,
  useDisclosure,
} from '@chakra-ui/react'

import type { Adapter } from '@/api/__generated__'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'
import MappingForm from '@/modules/Mappings/components/MappingForm.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { MappingType } from './types'

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

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const adapterId = selectedNode?.data.id

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'lg'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
        <DrawerHeader>
          <Text>{t('protocolAdapter.mapping.manager.header', { context: type })}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {!adapterId && <ErrorMessage message={t('protocolAdapter.error.loading')} />}
          {adapterId && (
            <MappingForm
              adapterId={adapterId}
              adapterType={selectedNode?.data.type}
              type={type}
              onSubmit={handleClose}
            />
          )}
        </DrawerBody>
        <DrawerFooter>
          <Button variant="primary" type="submit" form="adapter-mapping-form">
            {t('protocolAdapter.mapping.actions.submit')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterMappingManager
