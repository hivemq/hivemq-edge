import { FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'

import { DomainTagList } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ManagerContextType } from '@/modules/Mappings/types.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import { customUniqueTagValidation } from '@/modules/Device/utils/validation.utils.ts'
import { useListDomainTags } from '@/api/hooks/useDomainModel/useListDomainTags.ts'

interface DeviceTagFormProps {
  context: ManagerContextType<DomainTagList>
  onSubmit?: (data: DomainTagList | undefined) => void
}

const DeviceTagForm: FC<DeviceTagFormProps> = ({ context, onSubmit }) => {
  const { t } = useTranslation()
  const { data } = useListDomainTags()

  const allNames = (data?.items || []).map((e) => e.name)
  const initialNames = [...(context.formData?.items || [])].map((e) => e.name)
  // initial names have already been checked
  const cleanNames = allNames.filter((e) => !initialNames.includes(e))

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
      // @ts-ignore Need to fix that TS error
      customValidate={customUniqueTagValidation(cleanNames)}
    />
  )
}

export default DeviceTagForm
