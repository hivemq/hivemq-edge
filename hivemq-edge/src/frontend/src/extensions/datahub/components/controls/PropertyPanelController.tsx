import { useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'

import {
  AbsoluteCenter,
  Avatar,
  Box,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  Icon,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuConstruction } from 'react-icons/lu'

import NodeIcon from '../helpers/NodeIcon.tsx'

const PropertyPanelController = () => {
  const { t } = useTranslation('datahub')
  const { type, nodeId } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()

  useEffect(() => {
    if (type && nodeId) {
      onOpen()
    }
  }, [onOpen, type, nodeId])

  const onDrawerClose = useCallback(() => {
    onClose()
    navigate(state?.origin || '/datahub')
  }, [onClose, navigate, state?.origin])

  return (
    <Drawer
      size={'lg'}
      isOpen={isOpen}
      placement="right"
      onClose={onDrawerClose}
      // finalFocusRef={btnRef}
    >
      <DrawerOverlay />
      <DrawerContent data-testid={'node-editor-content'}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Flex>
            <Avatar icon={<NodeIcon type={type} />} bg="gray.200" data-testid={'node-editor-icon'} />
            <Box ml="3">
              <Text fontWeight="bold" data-testid={'node-editor-name'}>
                {t('workspace.nodes.type', { context: type })}
              </Text>
              <Text fontSize="sm" data-testid={'node-editor-id'}>
                id: {nodeId}
              </Text>
            </Box>
          </Flex>
        </DrawerHeader>

        <DrawerBody>
          <AbsoluteCenter axis="both" data-testid={'node-editor-under-construction'}>
            <Icon as={LuConstruction} boxSize={100} />
          </AbsoluteCenter>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default PropertyPanelController
