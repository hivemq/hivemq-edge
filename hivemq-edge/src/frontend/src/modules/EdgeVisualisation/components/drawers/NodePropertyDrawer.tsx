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

import DisclaimerWIP from '@/components/DisclaimerWIP.tsx'
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
          <VStack gap={4} alignItems={'stretch'}>
            <DisclaimerWIP />
            <Metrics />

            <Box padding={6} boxShadow="md" bg="white">
              <SkeletonCircle size="10" />
              <SkeletonText mt="4" noOfLines={4} spacing="4" skeletonHeight="2" />
            </Box>
          </VStack>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default NodePropertyDrawer
