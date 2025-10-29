import type { FC } from 'react'
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  HStack,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Text,
  VStack,
  useTheme,
} from '@chakra-ui/react'
import { LuLightbulb } from 'react-icons/lu'
import { useReactFlow } from '@xyflow/react'

import type { Combiner } from '@/api/__generated__'
import { ANIMATION } from '@/modules/Theme/utils'

import CombinerMappingsList from './CombinerMappingsList'

interface DuplicateCombinerModalProps {
  isOpen: boolean
  onClose: () => void
  existingCombiner: Combiner
  onUseExisting: () => void
  onCreateNew: () => void
  isAssetMapper?: boolean
}

/**
 * Modal displayed when user attempts to create a combiner that already exists
 * with the same source connections. Provides options to:
 * - Use the existing combiner (recommended)
 * - Create a new combiner anyway
 * - Cancel the operation
 */
const DuplicateCombinerModal: FC<DuplicateCombinerModalProps> = ({
  isOpen,
  onClose,
  existingCombiner,
  onUseExisting,
  onCreateNew,
  isAssetMapper = false,
}) => {
  const { t } = useTranslation()
  const theme = useTheme()
  const { fitView } = useReactFlow()
  const initialFocusRef = useRef<HTMLButtonElement>(null)

  // Animate focus to the existing combiner when modal opens
  useEffect(() => {
    if (isOpen && existingCombiner?.id) {
      // Small delay to allow modal animation to start
      const timer = setTimeout(() => {
        fitView({
          nodes: [{ id: existingCombiner.id }],
          padding: 2,
          duration: ANIMATION.FIT_VIEW_DURATION_MS,
        })
      }, 150)

      return () => clearTimeout(timer)
    }
  }, [isOpen, existingCombiner?.id, fitView])

  const handleUseExisting = () => {
    onClose()
    onUseExisting()
  }

  const handleCreateNew = () => {
    onClose()
    onCreateNew()
  }

  const modalTitle = isAssetMapper
    ? t('workspace.modal.duplicateCombiner.title.assetMapper')
    : t('workspace.modal.duplicateCombiner.title.combiner')

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="xl"
      initialFocusRef={initialFocusRef}
      closeOnOverlayClick={false}
      motionPreset="slideInBottom"
    >
      <ModalOverlay />
      <ModalContent
        data-testid="duplicate-combiner-modal"
        containerProps={{
          justifyContent: 'flex-start',
          alignItems: 'flex-start',
          pt: 20,
          pl: 20,
        }}
      >
        <ModalHeader>
          <HStack spacing={3}>
            <LuLightbulb color={theme.colors.blue[500]} size={24} />
            <VStack align="flex-start" spacing={0}>
              <Text data-testid="modal-title">{modalTitle}</Text>
              <Text fontSize="sm" fontWeight="normal" color="chakra-subtle-text" data-testid="modal-combiner-name">
                &ldquo;{existingCombiner.name}&rdquo;
              </Text>
            </VStack>
          </HStack>
        </ModalHeader>
        <ModalCloseButton data-testid="modal-close-button" />

        <ModalBody>
          <VStack align="stretch" spacing={4}>
            <Text data-testid="modal-description">
              {isAssetMapper
                ? t('workspace.modal.duplicateCombiner.description.assetMapper')
                : t('workspace.modal.duplicateCombiner.description.combiner')}
            </Text>

            <VStack align="stretch" spacing={2}>
              <Text fontWeight="semibold" fontSize="sm" data-testid="modal-mappings-label">
                {t('workspace.modal.duplicateCombiner.existingMappings')}
              </Text>
              <CombinerMappingsList mappings={existingCombiner.mappings?.items || []} />
            </VStack>

            <Text color="chakra-subtle-text" data-testid="modal-prompt">
              {t('workspace.modal.duplicateCombiner.prompt')}
            </Text>
          </VStack>
        </ModalBody>

        <ModalFooter>
          <HStack spacing={3} width="full" justifyContent="flex-end">
            <Button variant="ghost" onClick={onClose} data-testid="modal-button-cancel">
              {t('action.cancel')}
            </Button>
            <Button variant="outline" onClick={handleCreateNew} data-testid="modal-button-create-new">
              {t('workspace.modal.duplicateCombiner.actions.createNew')}
            </Button>
            <Button
              ref={initialFocusRef}
              variant="primary"
              onClick={handleUseExisting}
              data-testid="modal-button-use-existing"
            >
              {t('workspace.modal.duplicateCombiner.actions.useExisting')}
            </Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}

export default DuplicateCombinerModal
