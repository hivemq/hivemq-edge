import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardBody,
  CardHeader,
  HStack,
  List,
  ListItem,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import { LuBadgeHelp } from 'react-icons/lu'

import ShortcutRenderer from '@/components/Chakra/ShortcutRenderer.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { DATAHUB_HOTKEY_CONTEXT } from '@datahub/utils/datahub.utils.ts'
import type { HotKeyItem } from '@datahub/types.ts'

const DesignerCheatSheet: FC = () => {
  const { t } = useTranslation('datahub')
  const { isOpen, onOpen, onClose } = useDisclosure()

  const groupedKeys = useMemo(() => {
    return DATAHUB_HOTKEY_CONTEXT.reduce<Record<string, HotKeyItem[]>>((acc, item) => {
      if (!acc[item.category]) {
        acc[item.category] = []
      }

      acc[item.category].push(item)
      return acc
    }, {})
  }, [])

  return (
    <>
      <IconButton
        icon={<LuBadgeHelp />}
        onClick={onOpen}
        aria-label={t('workspace.controls.shortcuts')}
        data-testid="canvas-control-help"
      />
      <Modal isOpen={isOpen} onClose={onClose} size="2xl" isCentered motionPreset="scale" scrollBehavior="inside">
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>
            <Text fontSize="md">{t(`shortcuts.header`)}</Text>
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody overflowY="scroll" tabIndex={0}>
            <HStack gap={4} alignItems="flex-start">
              {Object.entries(groupedKeys).map(([group, keys]) => (
                <Card key={group} role="group" aria-labelledby={`group-${group}`} flex={1}>
                  <CardHeader id={`group-${group}`} p={2} borderBottomWidth={1}>
                    {t(`shortcuts.categories.${group}`)}
                  </CardHeader>
                  <CardBody p={2}>
                    <List spacing={3}>
                      {keys.map((item) => (
                        <ListItem key={`${group}-${item.key}`}>
                          <ShortcutRenderer
                            hotkeys={item.key}
                            description={t(`shortcuts.keys.${item.key}`, { context: item.category })}
                          />
                        </ListItem>
                      ))}
                    </List>
                  </CardBody>
                </Card>
              ))}
            </HStack>
          </ModalBody>
          <ModalFooter />
        </ModalContent>
      </Modal>
    </>
  )
}

export default DesignerCheatSheet
