import { FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'

import { DomainTagList } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ManagerContextType } from '@/modules/Mappings/types.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'

interface DeviceTagFormProps {
  context: ManagerContextType
  onSubmit?: (data: DomainTagList | undefined) => void
}

const DeviceTagForm: FC<DeviceTagFormProps> = ({ context, onSubmit }) => {
  const { t } = useTranslation()

  const onFormSubmit = useCallback(
    (data: IChangeEvent<DomainTagList>) => {
      onSubmit?.(data.formData)
    },
    [onSubmit]
  )

  if (!context.schema) return <ErrorMessage message={t('device.errors.noFormSchema')} />

  return (
    <ChakraRJSForm
      id="domainTags-instance-form"
      schema={context.schema}
      uiSchema={context.uiSchema}
      formData={context.formData}
      onSubmit={onFormSubmit}
    />
  )
}

export default DeviceTagForm
