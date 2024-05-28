import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import {
  Button,
  ButtonGroup,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  useDisclosure,
} from '@chakra-ui/react'
import { LuHardDriveUpload } from 'react-icons/lu'
import { UploadStepper } from '@/components/rjsf/BatchSubscription/components/UploadStepper.tsx'
import { useBatchModeSteps } from '@/components/rjsf/BatchSubscription/hooks/useBatchModeSteps.tsx'

export interface BatchSubscriptionProps {
  schema: RJSFSchema
}

const BatchUploadButton: FC<BatchSubscriptionProps> = () => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { t } = useTranslation('components')
  const { activeStep, steps, isStepCompleted, goToNext, goToPrevious } = useBatchModeSteps()

  return (
    <>
      <Button onClick={onOpen} leftIcon={<LuHardDriveUpload />} data-testid="array-field-batch-cta">
        {t('rjsf.batchUpload.button')}
      </Button>
      <Modal
        id="array-field-batch"
        size="3xl"
        closeOnOverlayClick={false}
        // initialFocusRef={cancelRef}
        // finalFocusRef={cancelRef}
        isOpen={isOpen}
        onClose={onClose}
        scrollBehavior="inside"
      >
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>{t('rjsf.batchUpload.modal.header')}</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            <UploadStepper steps={steps} activeStep={activeStep} />
          </ModalBody>
          <ModalFooter>
            <ButtonGroup>
              <Button onClick={goToPrevious} isDisabled={!activeStep}>
                {t('rjsf.batchUpload.modal.action.previous')}
              </Button>
              <Button onClick={goToNext} isDisabled={activeStep === steps.length - 1 || !isStepCompleted(activeStep)}>
                {t('rjsf.batchUpload.modal.action.next')}
              </Button>
              <Button onClick={onClose}>{t('rjsf.batchUpload.modal.action.cancel')}</Button>
            </ButtonGroup>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  )
}

export default BatchUploadButton
