import { useSteps } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useCallback } from 'react'


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
  const { isCompleteStep, isIncompleteStep, ...stepper } = useSteps()

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const isStepCompleted = useCallback((_: BatchModeStep): boolean => {
    return true
  }, [])

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
    isStepCompleted,
    steps,
  }
}
