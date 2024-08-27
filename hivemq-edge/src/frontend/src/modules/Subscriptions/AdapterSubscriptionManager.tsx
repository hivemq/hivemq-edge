import { type FC, useEffect, useMemo } from 'react'
import { Node, useNodes } from 'reactflow'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  IconButton,
  Text,
  useBoolean,
  useDisclosure,
} from '@chakra-ui/react'
import { LuExpand, LuShrink } from 'react-icons/lu'

import type { Adapter } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import SubscriptionForm from '@/modules/Subscriptions/components/SubscriptionForm.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface AdapterSubscriptionManagerProps {
  type: 'inward' | 'outward'
}

const AdapterSubscriptionManager: FC<AdapterSubscriptionManagerProps> = ({ type }) => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { nodeId } = useParams()
  const [isExpanded, setExpanded] = useBoolean(false)
  const nodes = useNodes()

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === nodeId && node.type === NodeTypes.ADAPTER_NODE) as Node<Adapter> | undefined
  }, [nodeId, nodes])

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    if (selectedNode) onOpen()
  }, [onOpen, selectedNode])

  if (!selectedNode?.data.id) return <ErrorMessage type="SSS" message="Dds" />

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'md'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <IconButton
          aria-label="expand"
          variant="ghost"
          colorScheme="gray"
          onClick={setExpanded.toggle}
          icon={isExpanded ? <LuShrink /> : <LuExpand />}
          style={{
            position: 'absolute',
            top: 'var(--chakra-space-2)',
            right: 0,
            width: '32px',
            height: '32px',
            transform: 'translate(-48px, 0)',
            minWidth: 'inherit',
          }}
        >
          Expand
        </IconButton>
        <DrawerHeader>
          <Text>Manage {type} subscriptions</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {nodeId && <SubscriptionForm id={selectedNode.data.id} type={type} />}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterSubscriptionManager
