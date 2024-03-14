import { FC, useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { IChangeEvent } from '@rjsf/core'

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
import { LuConstruction } from 'react-icons/lu'

import {
  BehaviorPolicyPanel,
  ClientFilterPanel,
  OperationPanel,
  SchemaPanel,
  TopicFilterPanel,
  TransitionPanel,
  ValidatorPanel,
} from '../panels'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { DataHubNodeType, PanelProps } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'

const DefaultEditor: Record<string, FC<PanelProps>> = {
  [DataHubNodeType.TOPIC_FILTER]: TopicFilterPanel,
  [DataHubNodeType.CLIENT_FILTER]: ClientFilterPanel,
  [DataHubNodeType.VALIDATOR]: ValidatorPanel,
  [DataHubNodeType.SCHEMA]: SchemaPanel,
  [DataHubNodeType.BEHAVIOR_POLICY]: BehaviorPolicyPanel,
  [DataHubNodeType.TRANSITION]: TransitionPanel,
  [DataHubNodeType.OPERATION]: OperationPanel,
}

const PropertyPanelController = () => {
  const { t } = useTranslation('datahub')
  const { onUpdateNodes } = useDataHubDraftStore()

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

  const Editor = type ? DefaultEditor[type] : null
  const isEditorValid = Editor && nodeId

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const { formData } = data
      if (nodeId) onUpdateNodes(nodeId, formData)
      onClose?.()
    },
    [nodeId, onUpdateNodes, onClose]
  )

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
          {isEditorValid ? (
            <Editor selectedNode={nodeId} onFormSubmit={onFormSubmit} />
          ) : (
            <AbsoluteCenter axis="both" data-testid={'node-editor-under-construction'}>
              <Icon as={LuConstruction} boxSize={100} />
            </AbsoluteCenter>
          )}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default PropertyPanelController
