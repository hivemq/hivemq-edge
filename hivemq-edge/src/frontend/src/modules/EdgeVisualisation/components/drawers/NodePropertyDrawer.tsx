import { FC, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useNodes } from 'reactflow'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  useDisclosure,
} from '@chakra-ui/react'
import Metrics from '@/modules/Welcome/components/Metrics.tsx'

const NodePropertyDrawer: FC = () => {
  const { nodeId, nodeType } = useParams()
  const navigate = useNavigate()

  const nodes = useNodes()
  const { isOpen, onOpen, onClose } = useDisclosure()

  useEffect(() => {
    const selected = nodes.filter((e) => e.id === nodeId)
    if (!nodeId || !selected) {
      navigate('/edge-flow')
      return
    }
    onOpen()
  }, [onOpen, navigate, nodeId])

  const handleClose = () => {
    onClose()
    navigate('/edge-flow')
  }

  return (
    <Drawer isOpen={isOpen} placement="right" size={'md'} onClose={handleClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>
          {nodeType}: {nodeId}
        </DrawerHeader>

        <DrawerBody>
          <Metrics />
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default NodePropertyDrawer
