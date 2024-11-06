import { useSteps } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useCallback, useState } from 'react'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { IdSchema } from '@rjsf/utils'

import { BatchModeStepType, BatchModeSteps, BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import DataSourceStep from '@/components/rjsf/BatchModeMappings/components/DataSourceStep.tsx'
import MappingsValidationStep from '@/components/rjsf/BatchModeMappings/components/MappingsValidationStep.tsx'
import ColumnMatcherStep from '@/components/rjsf/BatchModeMappings/components/ColumnMatcherStep.tsx'
import ConfirmStep from '@/components/rjsf/BatchModeMappings/components/ConfirmStep.tsx'

export const useBatchModeSteps = (idSchema: IdSchema<unknown>, schema: RJSFSchema) => {
  const { t } = useTranslation('components')
  const { isCompleteStep, isIncompleteStep, ...stepper } = useSteps()
  const [store, setStore] = useState<BatchModeStore>({ idSchema, schema })

  const isStepCompleted = useCallback(
    (step: BatchModeStepType): boolean => {
      if (step === BatchModeStepType.UPLOAD) return Boolean(store.worksheet)
      if (step === BatchModeStepType.MATCH) return Boolean(store.mapping)
      if (step === BatchModeStepType.VALIDATE) return Boolean(store.subscriptions)

      return false
    },
    [store.mapping, store.subscriptions, store.worksheet]
  )

  const onContinue = useCallback((partialStore: Partial<BatchModeStore>) => {
    setStore((old) => ({ ...old, ...partialStore }))
  }, [])

  const steps: BatchModeSteps[] = [
    {
      id: BatchModeStepType.UPLOAD,
      title: t('rjsf.batchUpload.modal.step.upload.title'),
      description: t('rjsf.batchUpload.modal.step.upload.description'),
      renderer: DataSourceStep,
    },
    {
      id: BatchModeStepType.MATCH,
      title: t('rjsf.batchUpload.modal.step.match.title'),
      description: t('rjsf.batchUpload.modal.step.match.description'),
      renderer: ColumnMatcherStep,
    },
    {
      id: BatchModeStepType.VALIDATE,
      title: t('rjsf.batchUpload.modal.step.validate.title'),
      description: t('rjsf.batchUpload.modal.step.validate.description'),
      renderer: MappingsValidationStep,
    },
    {
      id: BatchModeStepType.CONFIRM,
      isFinal: true,
      title: t('rjsf.batchUpload.modal.step.confirm.title'),
      description: t('rjsf.batchUpload.modal.step.confirm.description'),
      renderer: ConfirmStep,
    },
  ]

  return {
    ...stepper,
    isStepCompleted,
    onContinue,
    steps,
    store,
  }
}
