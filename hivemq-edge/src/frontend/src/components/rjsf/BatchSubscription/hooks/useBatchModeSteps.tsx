import { useSteps } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useCallback, useState } from 'react'

import { BatchModeStep, BatchModeSteps, BatchModeStore } from '@/components/rjsf/BatchSubscription/types.ts'
import DataSourceStep from '@/components/rjsf/BatchSubscription/components/DataSourceStep.tsx'
import SubscriptionsValidationStep from '@/components/rjsf/BatchSubscription/components/SubscriptionsValidationStep.tsx'
import ColumnMatcherStep from '@/components/rjsf/BatchSubscription/components/ColumnMatcherStep.tsx'
import ConfirmStep from '@/components/rjsf/BatchSubscription/components/ConfirmStep.tsx'

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
      renderer: DataSourceStep,
    },
    {
      id: BatchModeStep.MATCH,
      title: t('rjsf.batchUpload.modal.step.match.title'),
      description: t('rjsf.batchUpload.modal.step.match.description'),
      renderer: ColumnMatcherStep,
    },
    {
      id: BatchModeStep.VALIDATE,
      title: t('rjsf.batchUpload.modal.step.validate.title'),
      description: t('rjsf.batchUpload.modal.step.validate.description'),
      renderer: SubscriptionsValidationStep,
    },
    {
      id: BatchModeStep.CONFIRM,
      isFinal: true,
      title: t('rjsf.batchUpload.modal.step.confirm.title'),
      description: t('rjsf.batchUpload.modal.step.confirm.description'),
      renderer: ConfirmStep,
    },
  ]

  return {
    ...stepper,
    isStepCompleted,
    steps,
  }
}
