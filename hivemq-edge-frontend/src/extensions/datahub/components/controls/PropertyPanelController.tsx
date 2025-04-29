import { useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'

import {
  AbsoluteCenter,
  Avatar,
  Box,
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  Icon,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import { LuConstruction } from 'react-icons/lu'

import { DefaultEditor } from '@datahub/config/editors.config.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { NodeIcon } from '@datahub/components/helpers'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

const PropertyPanelController = () => {
  const { t } = useTranslation('datahub')
  const { type, nodeId } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { onUpdateNodes } = useDataHubDraftStore()
  const { isNodeEditable } = usePolicyGuards(nodeId)

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
      onDrawerClose()
    },
    [nodeId, onUpdateNodes, onDrawerClose]
  )

  return (
    <Drawer
      size="lg"
      isOpen={isOpen}
      placement="right"
      onClose={onDrawerClose}
      // finalFocusRef={btnRef}
    >
      <DrawerOverlay />
      <DrawerContent data-testid="node-editor-content">
        <DrawerCloseButton />
        <DrawerHeader>
          <Flex>
            <Avatar icon={<NodeIcon type={type} />} bg="gray.200" data-testid="node-editor-icon" />
            <Box ml="3">
              <Text fontWeight="bold" data-testid="node-editor-name">
                {t('workspace.nodes.type', { context: type })}
              </Text>
              <Text fontSize="sm" data-testid="node-editor-id">
                id: {nodeId}
              </Text>
            </Box>
          </Flex>
        </DrawerHeader>

        <DrawerBody>
          {isEditorValid ? (
            <Editor selectedNode={nodeId} onFormSubmit={onFormSubmit} />
          ) : (
            <AbsoluteCenter axis="both" data-testid="node-editor-under-construction">
              <Icon as={LuConstruction} boxSize={100} />
            </AbsoluteCenter>
          )}
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px">
          {isEditorValid && (
            <Flex flexGrow={1} justifyContent="flex-end">
              <Button variant="primary" type="submit" form="datahub-node-form" isDisabled={!isNodeEditable}>
                {t('workspace.panel.submit')}
              </Button>
            </Flex>
          )}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default PropertyPanelController
