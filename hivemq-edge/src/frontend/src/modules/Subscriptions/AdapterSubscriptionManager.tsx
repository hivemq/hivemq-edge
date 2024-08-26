import { type FC, useEffect } from 'react'
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

interface AdapterSubscriptionManagerProps {
  type: 'inward' | 'outward'
}

const AdapterSubscriptionManager: FC<AdapterSubscriptionManagerProps> = ({ type }) => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { nodeType, nodeId } = useParams()
  const [isExpanded, setExpanded] = useBoolean(false)

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    console.log('XXXXXX node', nodeType, nodeId)
    onOpen()
  }, [nodeId, nodeType, onOpen])

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'sm'} onClose={handleClose} variant="hivemq">
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
        <DrawerBody display="flex" flexDirection="column" gap={6}></DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterSubscriptionManager
