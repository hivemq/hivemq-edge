/**
 * Layout Presets Manager
 *
 * Component for saving and loading custom layout presets.
 * Allows users to save current node positions as named presets.
 */

import type { FC } from 'react'
import { useState } from 'react'
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
} from '@chakra-ui/react'
import { LuBookmark, LuSave, LuTrash2 } from 'react-icons/lu'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import type { LayoutPreset } from '../../types/layout.ts'

const LayoutPresetsManager: FC = () => {
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
      id: `preset-${Date.now()}`,
      name: presetName.trim(),
      createdAt: new Date(),
      algorithmType: layoutConfig.currentAlgorithm,
      options: layoutConfig.options,
      nodePositions: nodes.reduce(
        (acc, node) => {
          acc[node.id] = {
            x: node.position.x,
            y: node.position.y,
          }
          return acc
        },
        {} as Record<string, { x: number; y: number }>
      ),
    }

    saveLayoutPreset(preset)

    toast({
      title: 'Preset saved',
      description: `"${presetName}" saved successfully`,
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
        title: 'Preset loaded',
        description: `"${preset.name}" applied successfully`,
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
      title: 'Preset deleted',
      description: `"${preset?.name}" removed`,
      status: 'info',
      duration: 3000,
      isClosable: true,
    })
  }

  return (
    <>
      <Menu>
        <Tooltip label="Saved Presets" placement="bottom">
          <MenuButton
            as={IconButton}
            icon={<Icon as={LuBookmark} />}
            size="sm"
            variant="ghost"
            aria-label="Layout presets"
          />
        </Tooltip>
        <MenuList>
          <MenuItem icon={<Icon as={LuSave} />} onClick={onOpen}>
            Save Current Layout
          </MenuItem>

          {layoutConfig.presets.length > 0 && (
            <>
              <MenuDivider />
              <Text fontSize="xs" fontWeight="bold" px={3} py={1} color="gray.500">
                Saved Presets
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
                    aria-label="Delete preset"
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
                No saved presets
              </Text>
            </>
          )}
        </MenuList>
      </Menu>

      <Modal isOpen={isOpen} onClose={onClose}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Save Layout Preset</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            <FormControl>
              <FormLabel>Preset Name</FormLabel>
              <Input
                placeholder="e.g., My Custom Layout"
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
              This will save the current node positions and layout settings.
            </Text>
          </ModalBody>

          <ModalFooter>
            <Button variant="ghost" mr={3} onClick={onClose}>
              Cancel
            </Button>
            <Button colorScheme="blue" onClick={handleSavePreset} leftIcon={<Icon as={LuSave} />}>
              Save
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  )
}

export default LayoutPresetsManager
