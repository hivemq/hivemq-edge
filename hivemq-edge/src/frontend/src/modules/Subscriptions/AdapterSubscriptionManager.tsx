import { type FC, useEffect, useMemo } from 'react'
import { Node } from 'reactflow'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useBoolean,
  useDisclosure,
} from '@chakra-ui/react'

import type { Adapter } from '@/api/__generated__'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'
import SubscriptionForm from '@/modules/Subscriptions/components/SubscriptionForm.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface AdapterSubscriptionManagerProps {
  type: 'inward' | 'outward'
}

// TODO[NVL] Ensure layout fully responsive
const AdapterSubscriptionManager: FC<AdapterSubscriptionManagerProps> = ({ type }) => {
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
          <Text>Manage {type} subscriptions</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {!adapterId && <ErrorMessage message={t('protocolAdapter.error.loading')} />}
          {adapterId && <SubscriptionForm id={adapterId} type={type} />}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterSubscriptionManager
