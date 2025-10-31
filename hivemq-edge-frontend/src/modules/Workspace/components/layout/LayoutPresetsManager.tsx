/**
 * Layout Presets Manager
 *
 * Component for saving and loading custom layout presets.
 * Allows users to save current node positions as named presets.
 */

import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuDivider,
  Button,
  Icon,
  IconButton,
  useDisclosure,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
  ModalCloseButton,
  FormControl,
  FormLabel,
  Input,
  VStack,
  HStack,
  Text,
  useToast,
  Tooltip,
  Portal,
} from '@chakra-ui/react'
import { LuBookmark, LuSave, LuTrash2 } from 'react-icons/lu'
import { v4 as uuidv4 } from 'uuid'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import type { LayoutPreset } from '../../types/layout.ts'

const LayoutPresetsManager: FC = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [presetName, setPresetName] = useState('')
  const { nodes, layoutConfig, saveLayoutPreset, loadLayoutPreset, deleteLayoutPreset } = useWorkspaceStore()

  const handleSavePreset = () => {
    if (!presetName.trim()) {
      toast({
        title: 'Name required',
        description: 'Please enter a name for the preset',
        status: 'warning',
        duration: 3000,
        isClosable: true,
      })
      return
    }

    const preset: LayoutPreset = {
      id: `preset-${uuidv4()}`,
      name: presetName.trim(),
      createdAt: new Date(),
      updatedAt: new Date(),
      algorithm: layoutConfig.currentAlgorithm,
      options: layoutConfig.options,
      positions: new Map(
        nodes.map((node) => [
          node.id,
          {
            x: node.position.x,
            y: node.position.y,
          },
        ])
      ),
    }

    saveLayoutPreset(preset)

    toast({
      description: t('workspace.autoLayout.presets.toast.saved', { name: presetName }),
      status: 'success',
      duration: 3000,
      isClosable: true,
    })

    setPresetName('')
    onClose()
  }

  const handleLoadPreset = (presetId: string) => {
    loadLayoutPreset(presetId)
    const preset = layoutConfig.presets.find((p) => p.id === presetId)

    if (preset) {
      toast({
        description: t('workspace.autoLayout.presets.toast.loaded', { name: preset.name }),
        status: 'success',
        duration: 3000,
        isClosable: true,
      })
    }
  }

  const handleDeletePreset = (presetId: string) => {
    const preset = layoutConfig.presets.find((p) => p.id === presetId)
    deleteLayoutPreset(presetId)

    toast({
      description: t('workspace.autoLayout.presets.toast.removed', { name: preset?.name }),
      status: 'info',
      duration: 3000,
      isClosable: true,
    })
  }

  return (
    <>
      <Menu>
        <Tooltip label={t('workspace.autoLayout.presets.tooltip')} placement="bottom">
          <MenuButton
            data-testid="workspace-preset-trigger"
            as={IconButton}
            icon={<Icon as={LuBookmark} />}
            size="sm"
            variant="ghost"
            aria-label={t('workspace.autoLayout.presets.aria-label')}
          />
        </Tooltip>
        <Portal>
          <MenuList>
            <MenuItem icon={<Icon as={LuSave} />} onClick={onOpen}>
              {t('workspace.autoLayout.presets.actions.save')}
            </MenuItem>

            {layoutConfig.presets.length > 0 && (
              <>
                <MenuDivider />
                <Text fontSize="xs" fontWeight="bold" px={3} py={1} color="gray.500">
                  {t('workspace.autoLayout.presets.list.title')}
                </Text>

                {layoutConfig.presets.map((preset) => (
                  <HStack key={preset.id} spacing={0} _hover={{ bg: 'gray.50', _dark: { bg: 'gray.700' } }}>
                    <MenuItem flex={1} onClick={() => handleLoadPreset(preset.id)}>
                      <VStack align="start" spacing={0}>
                        <Text fontSize="sm">{preset.name}</Text>
                        <Text fontSize="xs" color="gray.500">
                          {new Date(preset.createdAt).toLocaleDateString()}
                        </Text>
                      </VStack>
                    </MenuItem>
                    <IconButton
                      icon={<Icon as={LuTrash2} />}
                      size="xs"
                      variant="ghost"
                      colorScheme="red"
                      aria-label={t('workspace.autoLayout.presets.actions.delete')}
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDeletePreset(preset.id)
                      }}
                    />
                  </HStack>
                ))}
              </>
            )}

            {layoutConfig.presets.length === 0 && (
              <>
                <MenuDivider />
                <Text fontSize="xs" px={3} py={2} color="gray.500">
                  {t('workspace.autoLayout.presets.list.empty')}
                </Text>
              </>
            )}
          </MenuList>
        </Portal>
      </Menu>

      <Modal isOpen={isOpen} onClose={onClose}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>{t('workspace.autoLayout.presets.modal.title')}</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            <FormControl>
              <FormLabel>{t('workspace.autoLayout.presets.modal.nameLabel')}</FormLabel>
              <Input
                placeholder={t('workspace.autoLayout.presets.modal.namePlaceholder')}
                data-testid="workspace-preset-input"
                value={presetName}
                onChange={(e) => setPresetName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleSavePreset()
                  }
                }}
              />
            </FormControl>
            <Text fontSize="sm" color="gray.500" mt={3}>
              {t('workspace.autoLayout.presets.modal.description')}
            </Text>
          </ModalBody>

          <ModalFooter>
            <Button data-testid="workspace-preset-cancel" variant="ghost" mr={3} onClick={onClose}>
              {t('workspace.autoLayout.presets.modal.cancel')}
            </Button>
            <Button
              data-testid="workspace-preset-save"
              colorScheme="blue"
              onClick={handleSavePreset}
              leftIcon={<Icon as={LuSave} />}
            >
              {t('workspace.autoLayout.presets.modal.save')}
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  )
}

export default LayoutPresetsManager
