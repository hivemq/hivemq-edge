import { useSteps } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

export enum BatchModeStep {
  UPLOAD,
  MATCH,
  VALIDATE,
}

export interface BatchModeSteps {
  id: BatchModeStep
  title: string
  description: string
}

export const useBatchModeSteps = () => {
  const { t } = useTranslation('components')
  const stepper = useSteps()

  const steps: BatchModeSteps[] = [
    {
      id: BatchModeStep.UPLOAD,
      title: t('rjsf.batchUpload.modal.step.upload.title'),
      description: t('rjsf.batchUpload.modal.step.upload.description'),
    },
    {
      id: BatchModeStep.MATCH,
      title: t('rjsf.batchUpload.modal.step.match.title'),
      description: t('rjsf.batchUpload.modal.step.match.description'),
    },
    {
      id: BatchModeStep.VALIDATE,
      title: t('rjsf.batchUpload.modal.step.validate.title'),
      description: t('rjsf.batchUpload.modal.step.validate.description'),
    },
  ]

  return {
    ...stepper,
    steps,
  }
}
